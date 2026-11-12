import fs from "node:fs/promises";
import path from "node:path";
import readline from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";
import { chromium } from "playwright";

const DEFAULT_START_URL = "https://sis.cuhk.edu.cn";
const ROOT = path.resolve("outputs", "sis_course_outlines");
const PROFILE_DIR = path.resolve("work", "sis-playwright-profile");
const WAIT_MS = 800;

function argValue(name, fallback = "") {
  const idx = process.argv.indexOf(name);
  return idx >= 0 && process.argv[idx + 1] ? process.argv[idx + 1] : fallback;
}

const START_URL = argValue("--start-url", DEFAULT_START_URL);
const LETTERS = argValue("--letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
  .split(/[,\s]+/)
  .flatMap((part) => part.split(""))
  .map((letter) => letter.toUpperCase())
  .filter((letter) => /^[A-Z]$/.test(letter));
const SKIP_COURSE_CODES = new Set(["ECE4006", "ECE4007", "GLB3030"]);
let RESUME_FROM_COURSE = argValue("--from", argValue("--resume-from", "")).trim().toUpperCase();

let lastDialogMessage = "";

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function safeName(value, fallback = "untitled") {
  return (
    String(value || fallback)
      .replace(/[<>:"/\\|?*\x00-\x1f]/g, "_")
      .replace(/\s+/g, " ")
      .trim()
      .slice(0, 140) || fallback
  );
}

function csvEscape(value) {
  return `"${String(value ?? "").replaceAll('"', '""')}"`;
}

function idSelector(id) {
  return `[id="${String(id).replaceAll("\\", "\\\\").replaceAll('"', '\\"')}"]`;
}

async function appendCsv(file, row) {
  const headers = [
    "letter",
    "subject",
    "course_code",
    "course_number",
    "course_title",
    "status",
    "outline_json",
    "outline_text",
    "outline_html",
    "terms_status",
    "terms_text",
    "terms_html",
    "detail_json",
    "note",
  ];
  const exists = await fs
    .access(file)
    .then(() => true)
    .catch(() => false);
  if (!exists) await fs.writeFile(file, headers.map(csvEscape).join(",") + "\n", "utf8");
  await fs.appendFile(file, headers.map((h) => csvEscape(row[h])).join(",") + "\n", "utf8");
}

async function waitPS(page) {
  await page.waitForLoadState("domcontentloaded").catch(() => {});
  await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
  await page
    .locator("#WAIT_win0, .PSPROCESSING")
    .first()
    .waitFor({ state: "hidden", timeout: 8000 })
    .catch(() => {});
  await sleep(WAIT_MS);
}

async function activeCtx(page) {
  const contexts = [page, ...page.frames()];
  for (const ctx of contexts) {
    const hasCatalog =
      (await ctx.locator("#DERIVED_SSS_BCC_SSR_ALPHANUM_A").count().catch(() => 0)) ||
      (await ctx.locator("[id^='CRSE_TITLE$']").count().catch(() => 0)) ||
      (await ctx.getByText(/Browse Course Catalog|浏览课程目录/).count().catch(() => 0));
    const hasDetail =
      (await ctx.locator("#DERIVED_CRSECAT_DESCR200").count().catch(() => 0)) ||
      (await ctx.getByText(/View Course Outline|Course Outline|课程大纲/i).count().catch(() => 0));
    if (hasCatalog || hasDetail) return ctx;
  }
  const frameInfo = page.frames().map((f, i) => `${i}: ${f.url()}`).join("\n");
  throw new Error(`Cannot find SIS course catalog/detail frame.\nMain URL: ${page.url()}\nFrames:\n${frameInfo}`);
}

async function outlineCtx(page) {
  const contexts = [page, ...page.frames()];
  for (const ctx of contexts) {
    const text = await ctx.locator("body").innerText().catch(() => "");
    if (
      /Language of Instruction|Description \(English\)|Learning Outcomes|Course Syllabus|Assessment Scheme/i.test(
        text
      )
    ) {
      return ctx;
    }
  }
  return await activeCtx(page);
}

async function submitAction(page, actionId) {
  const ctx = await activeCtx(page);
  lastDialogMessage = "";
  const dialogHandler = async (dialog) => {
    lastDialogMessage = dialog.message();
    console.log(`    dialog: ${lastDialogMessage}`);
    await dialog.accept().catch(() => {});
  };
  page.once("dialog", dialogHandler);
  await ctx.evaluate((id) => {
    if (typeof window.submitAction_win0 === "function" && document.win0) {
      window.submitAction_win0(document.win0, id);
      return;
    }
    document.getElementById(id)?.click();
  }, actionId);
  await waitPS(page);
  page.removeListener("dialog", dialogHandler);
}

async function clickVisible(ctx, selector) {
  const loc = ctx.locator(selector).first();
  if ((await loc.count().catch(() => 0)) === 0) return false;
  if (!(await loc.isVisible().catch(() => false))) return false;
  await loc.click();
  return true;
}

async function pageText(page) {
  const contexts = [page, ...page.frames()];
  const chunks = [];
  for (const ctx of contexts) {
    const text = await ctx.locator("body").innerText().catch(() => "");
    if (text) chunks.push(text);
  }
  return chunks.join("\n");
}

function isNoOutlineText(text) {
  return /no\s*course\s*outline\s*for\s*this\s*course\s*currently|course outline.*currently|outline.*currently/i.test(
    text
  );
}

async function clickPeopleSoftOkIfPresent(page) {
  const contexts = [page, ...page.frames()];
  const selectors = [
    "#ICOK",
    "#OK",
    "[id*='OK']",
    "[id*='Ok']",
    "[id*='ok']",
    "input[value='OK']",
    "input[value='Ok']",
    "input[value='确定']",
    "input[value='确 定']",
    "input[title='确定']",
    "input[title='OK']",
    "button:has-text('OK')",
    "button:has-text('确定')",
    "a:has-text('OK')",
    "a:has-text('确定')",
    "text=确定",
    "text=OK",
  ];

  for (const ctx of contexts) {
    for (const selector of selectors) {
      const loc = ctx.locator(selector).first();
      if ((await loc.count().catch(() => 0)) > 0 && (await loc.isVisible().catch(() => false))) {
        await loc.click({ force: true }).catch(async () => {
          await loc.evaluate((el) => el.click());
        });
        await waitPS(page);
        await page.locator("#pt_modals, #pt_modalMask").waitFor({ state: "hidden", timeout: 5000 }).catch(() => {});
        return true;
      }
    }
  }

  for (const ctx of contexts) {
    const okHandle = await ctx
      .locator("a,input,button,span,div")
      .evaluateHandle((els) =>
        els.find((el) => {
          const text = `${el.textContent || ""} ${el.getAttribute("value") || ""} ${el.getAttribute("title") || ""}`
            .replace(/\s+/g, " ")
            .trim();
          const visible = !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
          return visible && /^(OK|Ok|确定|确 定)$/.test(text);
        }) || null
      )
      .catch(() => null);
    const element = okHandle ? okHandle.asElement() : null;
    if (element) {
      await element.click({ force: true }).catch(async () => {
        await element.evaluate((el) => el.click());
      });
      await waitPS(page);
      await page.locator("#pt_modals, #pt_modalMask").waitFor({ state: "hidden", timeout: 5000 }).catch(() => {});
      return true;
    }
  }

  await page.keyboard.press("Enter").catch(() => {});
  await waitPS(page);
  await page.locator("#pt_modals, #pt_modalMask").waitFor({ state: "hidden", timeout: 5000 }).catch(() => {});
  const remaining = await pageText(page);
  if (!isNoOutlineText(remaining)) return true;
  return false;
}

async function clickLetter(page, letter) {
  const actionId = `DERIVED_SSS_BCC_SSR_ALPHANUM_${letter}`;
  const ctx = await activeCtx(page);
  if ((await ctx.locator(idSelector(actionId)).count().catch(() => 0)) > 0) {
    await submitAction(page, actionId);
  } else {
    await ctx.getByText(letter, { exact: true }).click({ timeout: 10000 });
    await waitPS(page);
  }
}

async function expandAll(page) {
  for (let i = 0; i < 5; i += 1) {
    const ctx = await activeCtx(page);
    const actionId = await ctx.locator("a[id^='DERIVED_SSS_BCC_SSS_EXPAND_ALL']")
      .evaluateAll((links) => {
        const visible = links.filter((a) => {
          const r = a.getBoundingClientRect();
          return r.width > 0 && r.height > 0;
        });
        return visible.length ? visible[visible.length - 1].id : "";
      })
      .catch(() => "");
    if (actionId) {
      await submitAction(page, actionId);
    } else {
      const clicked =
        (await clickVisible(ctx, "text=展开全部")) || (await clickVisible(ctx, "text=Expand All"));
      if (!clicked) break;
      await waitPS(page);
    }
    for (let j = 0; j < 8; j += 1) {
      const newCtx = await activeCtx(page).catch(() => ctx);
      const titleCount = await newCtx.locator("[id^='CRSE_TITLE$']").count().catch(() => 0);
      if (titleCount > 0) return;
      await sleep(500);
    }
    const newCtx = await activeCtx(page).catch(() => ctx);
    const collapsed = await newCtx.locator("a[id^='DERIVED_SSS_BCC_SSR_EXPAND_COLLAPS']").count().catch(() => 0);
    if (collapsed === 0) break;
  }

  const ctx = await activeCtx(page);
  const titleCount = await ctx.locator("[id^='CRSE_TITLE$']").count().catch(() => 0);
  if (titleCount > 0) return;

  const subjectExpandIds = await ctx.locator("a[id^='DERIVED_SSS_BCC_SSR_EXPAND_COLLAPS']").evaluateAll((links) =>
    links
      .filter((a) => {
        const img = a.querySelector("img");
        const src = img?.getAttribute("src") || "";
        const r = a.getBoundingClientRect();
        return r.width > 0 && r.height > 0 && /EXPAND/i.test(src);
      })
      .map((a) => a.id)
  ).catch(() => []);
  for (const actionId of subjectExpandIds) {
    await submitAction(page, actionId).catch(() => {});
    const newCtx = await activeCtx(page).catch(() => ctx);
    const count = await newCtx.locator("[id^='CRSE_TITLE$']").count().catch(() => 0);
    if (count > 0) break;
  }
}

async function extractVisibleCourses(page) {
  const ctx = await activeCtx(page);
  return await ctx.evaluate(() => {
    const rows = [];
    let currentSubject = "";
    for (const el of document.querySelectorAll("td, span, a, div")) {
      const text = (el.textContent || "").replace(/\s+/g, " ").trim();
      const id = el.id || "";
      if (/^[A-Z]{2,5}\s+-\s+/.test(text)) currentSubject = text;
      const m = id.match(/^CRSE_TITLE\$(\d+)$/);
      if (!m) continue;
      const idx = m[1];
      const numEl = document.getElementById(`CRSE_NBR$span$${idx}`) || document.getElementById(`CRSE_NBR$${idx}`);
      const courseNumber = numEl ? numEl.textContent.replace(/\s+/g, " ").trim() : "";
      const subjectCode = (currentSubject.match(/^([A-Z]{2,5})\s+-/) || [])[1] || "";
      rows.push({
        subject: currentSubject,
        subjectCode,
        courseNumber,
        courseCode: subjectCode && courseNumber ? `${subjectCode}${courseNumber}` : courseNumber,
        courseTitle: text,
        actionId: id,
      });
    }
    const seen = new Set();
    return rows.filter((r) => {
      const key = `${r.subject}|${r.courseNumber}|${r.courseTitle}|${r.actionId}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  });
}

async function extractDetail(page) {
  const ctx = await activeCtx(page);
  return await ctx.evaluate(() => {
    const byId = (id) => (document.getElementById(id)?.textContent || "").replace(/\s+/g, " ").trim();
    return {
      courseHeading: byId("DERIVED_CRSECAT_DESCR200"),
      units: byId("DERIVED_CRSECAT_UNITS_RANGE$0"),
      gradingBasis: byId("SSR_CRSE_OFF_VW_GRADING_BASIS$0"),
      component: byId("DERIVED_CRSECAT_DESCR$0"),
      campus: byId("CAMPUS_TBL_DESCR$0"),
      school: byId("ACAD_GROUP_TBL_DESCR$0"),
      academicOrg: byId("ACAD_ORG_TBL_DESCR$0"),
      description: byId("SSR_CRSE_OFF_VW_DESCRLONG$0"),
      pageText: document.body.innerText.replace(/\s+/g, " ").trim(),
    };
  });
}

async function gotoCourseDetail(page, course) {
  await submitAction(page, course.actionId);
  await waitPS(page);
  const ctx = await activeCtx(page);
  const onDetail = await ctx.locator("#DERIVED_CRSECAT_DESCR200").count().catch(() => 0);
  if (!onDetail) throw new Error(`Did not enter course detail for ${course.courseCode}`);
}

async function returnToCatalog(page) {
  for (let i = 0; i < 3; i += 1) {
    const ctx = await activeCtx(page).catch(() => page);
    const actionId = await ctx
      .locator("a[id^='DERIVED_SAA_CRS_RETURN_PB']")
      .first()
      .getAttribute("id")
      .catch(() => null);
    if (actionId) {
      await submitAction(page, actionId);
      await waitPS(page);
      const newCtx = await activeCtx(page).catch(() => null);
      if (newCtx && (await newCtx.locator("[id^='CRSE_TITLE$']").count().catch(() => 0)) > 0) return;
      continue;
    }
    if (
      (await clickVisible(ctx, "text=返回 浏览课程目录")) ||
      (await clickVisible(ctx, "text=Return to Browse Course Catalog"))
    ) {
      await waitPS(page);
      return;
    }
    await page.goBack().catch(() => {});
    await waitPS(page);
  }
}

async function returnFromOutlineToDetail(page) {
  const ctx = await activeCtx(page).catch(() => page);
  const returnLinks = await ctx.locator("a").evaluateAll((links) =>
    links
      .map((a) => ({ id: a.id, text: (a.textContent || "").replace(/\s+/g, " ").trim() }))
      .filter((x) => x.id && /^(返回|Return|Back)$/i.test(x.text))
  ).catch(() => []);
  if (returnLinks.length) {
    await submitAction(page, returnLinks[0].id);
    return;
  }
  await page.goBack().catch(() => {});
  await waitPS(page);
}

function sectionFromText(text, startRegex, endRegexes) {
  const normalized = text.replace(/\r/g, "\n");
  const m = normalized.match(startRegex);
  if (!m) return "";
  let rest = normalized.slice(m.index + m[0].length);
  let end = rest.length;
  for (const re of endRegexes) {
    const em = rest.match(re);
    if (em && em.index >= 0) end = Math.min(end, em.index);
  }
  return rest.slice(0, end).replace(/\n{3,}/g, "\n\n").trim();
}

function parseOutlineFields(text) {
  const ends = [
    /Language of Instruction/i,
    /Description \(English\)/i,
    /Description \(Chinese\)/i,
    /Prerequisites?/i,
    /Co-?requisites?/i,
    /Learning Outcomes?/i,
    /Course Syllabus/i,
    /Assessment Scheme/i,
    /Grade descriptor/i,
    /Grade Type/i,
    /Course components?/i,
  ];
  return {
    languageOfInstruction: sectionFromText(text, /Language of Instruction\s*:?\s*/i, ends),
    descriptionEnglish: sectionFromText(text, /Description \(English\)\s*:?\s*/i, ends),
    descriptionChinese: sectionFromText(text, /Description \(Chinese\)\s*:?\s*/i, ends),
    prerequisites: sectionFromText(text, /Prerequisites?\s*:?\s*/i, ends),
    coRequisites: sectionFromText(text, /Co-?requisites?\s*:?\s*/i, ends),
    learningOutcomes: sectionFromText(text, /Learning Outcomes?\s*:?\s*/i, ends),
    courseSyllabus: sectionFromText(text, /Course Syllabus\s*:?\s*/i, ends),
    assessmentScheme: sectionFromText(text, /Assessment Scheme\s*:?\s*/i, ends),
    gradeDescriptor: sectionFromText(text, /Grade descriptor\s*-?\s*Grade Type\s*:?\s*/i, ends),
    courseComponents: sectionFromText(text, /Course components?\s*:?\s*/i, ends),
  };
}

async function extractOutlinePayload(ctx) {
  return await ctx.evaluate(() => {
    const clean = (s) => String(s || "").replace(/\r/g, "\n").replace(/[ \t]+\n/g, "\n").replace(/\n{3,}/g, "\n\n").trim();
    const visible = (el) => !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
    const textOf = (el) => clean(el?.innerText || el?.textContent || "");
    const bodyText = clean(document.body.innerText || "");

    function controlValue(el) {
      if (!el) return "";
      const tag = el.tagName.toLowerCase();
      if (tag === "textarea") return clean(el.value);
      if (tag === "input") return clean(el.value);
      if (tag === "select") return clean(el.options?.[el.selectedIndex]?.text || el.value);
      if (tag === "table") return clean(el.innerText);
      return textOf(el);
    }

    function simplePairsFromTable(table) {
      if (!table) return [];
      const rows = [...table.querySelectorAll("tr")]
        .map((tr) =>
          [...tr.querySelectorAll("th,td")]
            .map((cell) => clean(cell.innerText || cell.textContent || ""))
            .filter(Boolean)
        )
        .filter((row) => row.length);
      // Keep only compact data rows, dropping PeopleSoft navigation/header noise.
      return rows
        .filter((row) => row.length <= 4)
        .filter((row) => !row.join(" ").match(/个性化|查找|第一页|最后一项|返回|Assessment Scheme|Grade descriptor|Course components/i));
    }

    function rowsFromTable(table) {
      if (!table) return [];
      return [...table.querySelectorAll("tr")]
        .map((tr) =>
          [...tr.querySelectorAll("th,td")]
            .map((cell) => clean(cell.innerText || cell.textContent || ""))
            .filter(Boolean)
        )
        .filter((row) => row.length);
    }

    function cleanRowsFromTable(table) {
      const rows = rowsFromTable(table);

      const compactRows = rows.filter((row) => {
        const joined = row.join(" ");
        if (row.length < 2 || row.length > 4) return false;
        if (joined.length > 260) return false;
        if (/Assessment Scheme|Grade descriptor|Course components|Feedback for evaluation|Reading|Indicative teaching plan|Academic Honesty/i.test(joined)) return false;
        if (/Personalize|Find|First|Last|View All|返回|个性化|查找|第一页|最后一项|最后一页/i.test(joined)) return false;
        return true;
      });

      const seen = new Set();
      return compactRows.filter((row) => {
        const key = row.join("|");
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      });
    }

    function rowsToText(rows) {
      return (rows || []).map((row) => row.join(" | ")).join("\n");
    }

    function rowsBetween(table, startRegex, stopRegex) {
      const rows = rowsFromTable(table);
      const out = [];
      let started = false;
      for (const row of rows) {
        const joined = row.join(" ");
        if (!started) {
          if (startRegex.test(joined)) started = true;
          continue;
        }
        if (stopRegex.test(joined)) break;
        out.push(row);
      }
      return out;
    }

    function allPageRows() {
      return [...document.querySelectorAll("tr")]
        .filter(visible)
        .map((tr) =>
          [...tr.querySelectorAll("th,td")]
            .map((cell) => clean(cell.innerText || cell.textContent || ""))
            .filter(Boolean)
        )
        .filter((row) => row.length);
    }

    function rowsBetweenAllRows(startRegex, stopRegex) {
      const rows = allPageRows();
      const out = [];
      let started = false;
      for (const row of rows) {
        const joined = row.join(" ");
        if (!started) {
          if (startRegex.test(joined)) started = true;
          continue;
        }
        if (stopRegex.test(joined)) break;
        out.push(row);
      }
      return out;
    }

    function numericRows(rows) {
      return (rows || []).filter((row) => {
        const joined = row.join(" ");
        const last = row[row.length - 1];
        if (row.length < 2 || row.length > 4) return false;
        if (!/^-?\d+(\.\d+)?%?$/.test(last)) return false;
        if (/Component\/method|%weight|Hours\/week|Activity|Grade Type|Description|Book Title|Week|Reading/i.test(joined)) return false;
        return true;
      });
    }

    function indexedPairsById(labelPrefix, valuePrefix) {
      const pairs = [];
      const labels = [...document.querySelectorAll(`span[id^="${labelPrefix}$"]`)];
      for (const labelEl of labels) {
        const match = labelEl.id.match(/\$(\d+)$/);
        if (!match) continue;
        const valueEl = document.getElementById(`${valuePrefix}$${match[1]}`);
        const label = clean(labelEl.innerText || labelEl.textContent || "");
        const value = clean(valueEl?.innerText || valueEl?.textContent || "");
        if (label && /^-?\d+(\.\d+)?%?$/.test(value)) pairs.push([label, value]);
      }
      return pairs;
    }

    function textBetween(text, startRegex, stopRegex) {
      const startMatch = text.match(startRegex);
      if (!startMatch || startMatch.index == null) return "";
      const start = startMatch.index + startMatch[0].length;
      const rest = text.slice(start);
      const stopMatch = rest.match(stopRegex);
      return clean(stopMatch && stopMatch.index != null ? rest.slice(0, stopMatch.index) : rest);
    }

    function numericRowsFromText(text) {
      const rows = [];
      for (const rawLine of String(text || "").split(/\n+/)) {
        const line = clean(rawLine.replace(/\t+/g, " "));
        if (!line) continue;
        if (/Component\/method|%weight|Hours\/week|Grade Type|Book Title|Week|Description|Prerequisites|Co-requisites/i.test(line)) continue;
        const match = line.match(/^(.+?)\s+(-?\d+(?:\.\d+)?%?)$/);
        if (!match) continue;
        const label = clean(match[1]);
        const value = clean(match[2]);
        if (!label || /^(A|A-|B\+|B|B-|C\+|C|C-|D\+|D|F)$/i.test(label)) continue;
        rows.push([label, value]);
      }
      return rows;
    }

    function tableByText(regex) {
      return [...document.querySelectorAll("table")]
        .filter(visible)
        .map((el) => ({ el, text: controlValue(el), r: el.getBoundingClientRect() }))
        .filter(({ text }) => regex.test(text))
        .filter(({ text }) => text.length < 3000)
        .sort((a, b) => a.r.top - b.r.top)[0]?.el;
    }

    function tableAfterLabel(labelRegex, tableRegex = /./) {
      const label = [...document.querySelectorAll("td,span,label,div")]
        .filter(visible)
        .find((el) => labelRegex.test(textOf(el)));
      if (!label) return null;
      const y = label.getBoundingClientRect().top;
      return [...document.querySelectorAll("table")]
        .filter(visible)
        .map((el) => ({ el, text: controlValue(el), r: el.getBoundingClientRect() }))
        .filter(({ text, r }) => r.top >= y - 20 && tableRegex.test(text))
        .filter(({ text }) => text.length < 3000)
        .sort((a, b) => a.r.top - b.r.top)[0]?.el || null;
    }

    function byLabel(labelRegex) {
      const labels = [...document.querySelectorAll("td,span,label,div")]
        .filter(visible)
        .filter((el) => labelRegex.test(textOf(el)));
      if (!labels.length) return "";

      const label = labels[0];
      const lr = label.getBoundingClientRect();
      const controls = [...document.querySelectorAll("textarea,input:not([type='hidden']),select,table,div")]
        .filter(visible)
        .map((el) => ({ el, r: el.getBoundingClientRect() }))
        .filter(({ el, r }) => {
          if (el === label || label.contains(el)) return false;
          const toRight = r.left >= lr.right - 20;
          const verticalOverlap = r.bottom >= lr.top - 12 && r.top <= lr.bottom + 180;
          const plausibleWidth = r.width > 50 || ["textarea", "table"].includes(el.tagName.toLowerCase());
          return toRight && verticalOverlap && plausibleWidth;
        })
        .sort((a, b) => Math.abs(a.r.top - lr.top) - Math.abs(b.r.top - lr.top) || a.r.left - b.r.left);
      return controlValue(controls[0]?.el);
    }

    function siblingTextByLabel(labelRegex) {
      const labels = [...document.querySelectorAll("td,th,span,label,div")]
        .filter(visible)
        .filter((el) => labelRegex.test(textOf(el)));
      for (const label of labels) {
        const row = label.closest("tr");
        if (!row) continue;
        const cells = [...row.querySelectorAll("th,td")]
          .map((cell) => clean(cell.innerText || cell.textContent || ""))
          .filter(Boolean);
        const idx = cells.findIndex((text) => labelRegex.test(text));
        if (idx >= 0 && cells[idx + 1]) return cells[idx + 1];
      }
      return "";
    }

    const textareas = [...document.querySelectorAll("textarea")]
      .filter(visible)
      .filter((el) => el.getBoundingClientRect().width > 120 && el.getBoundingClientRect().height > 25);

    const langMatch =
      bodyText.match(/Language of Instruction\s*\n+\s*([^\n]+?)\s*\n+\s*Description \(English\)/i) ||
      bodyText.match(/Language of Instruction\s+([^\n]+?)\s+Description \(English\)/i) ||
      bodyText.match(/Language of Instruction\s*\n+\s*([^\n]+)/i);

    const assessmentTable =
      tableByText(/Component\/method[\s\S]*%weight/i) ||
      tableAfterLabel(/^Assessment Scheme$/i, /Component\/method|%weight/i);
    const gradeTable =
      tableByText(/Grade Type[\s\S]*(Outstanding performance|Description|Note\/Remark)/i) ||
      tableAfterLabel(/^Grade descriptor$/i, /Grade Type|Outstanding performance/i);
    const componentsTable =
      tableAfterLabel(/^Course components$/i, /Hours\/week|Lecture|Tutorial|Laboratory|Seminar/i) ||
      tableByText(/Hours\/week[\s\S]*(Lecture|Tutorial|Laboratory|Seminar)/i);

    let assessmentSchemeItems = rowsBetween(
      assessmentTable,
      /Component\/method[\s\S]*%weight/i,
      /Grade Type|Grade descriptor|Book Title|Reading|Feedback for evaluation|活动|Activity|Week|Course components|Indicative teaching plan/i
    ).filter((row) => {
      const joined = row.join(" ");
      return row.length >= 2 && row.length <= 3 && /^-?\d+(\.\d+)?%?$/.test(row[row.length - 1]) && !/Component\/method|%weight/i.test(joined);
    });
    let courseComponentItems = rowsBetween(
      componentsTable,
      /Hours\/week|活动|Activity/i,
      /Week|Indicative teaching plan|Academic Honesty|Reading Type|Book Title/i
    ).filter((row) => {
      const joined = row.join(" ");
      return row.length >= 2 && row.length <= 3 && /^-?\d+(\.\d+)?%?$/.test(row[row.length - 1]) && !/Hours\/week|Activity|活动/i.test(joined);
    });
    if (!assessmentSchemeItems.length) {
      assessmentSchemeItems = numericRows(rowsBetweenAllRows(
        /Component\/method|%weight/i,
        /Grade Type|Grade descriptor|Feedback for evaluation|Reading Type|Book Title|Course components|Hours\/week|Indicative teaching plan/i
      ));
    }
    if (!assessmentSchemeItems.length) {
      assessmentSchemeItems = numericRowsFromText(textBetween(
        bodyText,
        /Component\/method[\s\S]{0,500}%weight/i,
        /Grade Type|Grade descriptor|Feedback for evaluation|Reading Type|Book Title|Course components|Indicative teaching plan/i
      ));
    }
    if (!courseComponentItems.length) {
      courseComponentItems = numericRows(rowsBetweenAllRows(
        /Hours\/week/i,
        /Week|Indicative teaching plan|Academic Honesty|Reading Type|Book Title/i
      ));
    }
    if (!courseComponentItems.length) {
      courseComponentItems = numericRowsFromText(textBetween(
        bodyText,
        /Course components[\s\S]{0,500}Hours\/week/i,
        /Indicative teaching plan|Academic Honesty|Reading Type|Book Title|Week Content/i
      ));
    }
    const assessmentRowsById = indexedPairsById("CUSZ_ASSESSM_CUSZ_LAM_TYPE_DESC", "CUSZ_ASSESSM_LAM_WEIGHT");
    if (assessmentRowsById.length) assessmentSchemeItems = assessmentRowsById;
    const courseComponentRowsById = indexedPairsById("CUSZ_CRSE_COMP_ACTIVITYNAME", "CUSZ_CRSE_COMP_CUSZ_HOURS");
    if (courseComponentRowsById.length) courseComponentItems = courseComponentRowsById;
    const gradeText = controlValue(gradeTable);
    const gradeTypeMatch = gradeText.match(/Grade Type:\s*([^\n\r]+)/i);
    let languageOfInstruction = clean(siblingTextByLabel(/^Language of Instruction$/i) || langMatch?.[1] || byLabel(/^Language of Instruction$/i));
    if (!languageOfInstruction || /^Description\b/i.test(languageOfInstruction)) {
      const languageMatch =
        bodyText.match(/Language of Instruction\s+(英语|English|英文|中文|Chinese|普通话|Mandarin|粤语|Cantonese)\s+Description \(English\)/i) ||
        bodyText.match(/Language of Instruction\s*\n+\s*(英语|English|英文|中文|Chinese|普通话|Mandarin|粤语|Cantonese)\s*\n+/i);
      languageOfInstruction = clean(languageMatch?.[1] || "");
    }

    const fields = {
      languageOfInstruction,
      descriptionEnglish: controlValue(textareas[0]) || byLabel(/^Description \(English\)$/i),
      descriptionChinese: controlValue(textareas[1]) || byLabel(/^Description \(Chinese\)$/i),
      prerequisites: controlValue(textareas[2]) || byLabel(/^Prerequisites$/i),
      coRequisites: controlValue(textareas[3]) || byLabel(/^Co-requisites$/i),
      learningOutcomes: controlValue(textareas[4]) || byLabel(/^Learning Outcomes$/i),
      courseSyllabus: controlValue(textareas[5]) || byLabel(/^Course Syllabus$/i),
      assessmentScheme: rowsToText(assessmentSchemeItems),
      gradeType: clean(gradeTypeMatch?.[1] || ""),
      courseComponents: rowsToText(courseComponentItems),
      assessmentSchemeItems,
      courseComponentItems,
      debugNumericRows: allPageRows()
        .filter((row) => row.some((cell) => /^-?\d+(\.\d+)?%?$/.test(cell)))
        .slice(0, 80),
      debugTables: [...document.querySelectorAll("table")]
        .filter(visible)
        .map((table) => clean(table.innerText || table.textContent || "").slice(0, 500))
        .filter(Boolean)
        .slice(0, 20),
    };

    return { bodyText, fields };
  });
}

function outlineFieldsToText(fields) {
  const sections = [
    ["Language of Instruction", fields.languageOfInstruction],
    ["Description (English)", fields.descriptionEnglish],
    ["Description (Chinese)", fields.descriptionChinese],
    ["Prerequisites", fields.prerequisites],
    ["Co-requisites", fields.coRequisites],
    ["Learning Outcomes", fields.learningOutcomes],
    ["Course Syllabus", fields.courseSyllabus],
    ["Assessment Scheme", fields.assessmentScheme],
    ["Grade Type", fields.gradeType],
    ["Course components", fields.courseComponents],
  ];
  return sections
    .map(([label, value]) => `${label}\n${String(value || "").trim()}`)
    .filter((section) => !section.endsWith("\n"))
    .join("\n\n");
}

async function findOutlineAction(page) {
  const ctx = await activeCtx(page);
  return await ctx
    .locator("#CUSZ_SAA_DVW_SSR_RSLT_OUTCOME, a:has-text('View Course Outline'), a:has-text('Course Outline')")
    .first()
    .getAttribute("id")
    .catch(() => null);
}

async function clickOutlineAndSave(page, courseFolder, courseCode) {
  const actionId = await findOutlineAction(page);
  if (!actionId) return { status: "ghost_no_outline_button", note: "No outline button" };

  const popupPromise = page.waitForEvent("popup", { timeout: 8000 }).catch(() => null);
  const downloadPromise = page.waitForEvent("download", { timeout: 8000 }).catch(() => null);
  await submitAction(page, actionId);

  if (lastDialogMessage) {
    if (/no\s*course\s*outline|course outline.*currently|outline.*currently/i.test(lastDialogMessage)) {
      return { status: "ghost_outline_unavailable", note: lastDialogMessage };
    }
    return { status: "outline_dialog", note: lastDialogMessage };
  }

  const afterClickText = await pageText(page);
  if (isNoOutlineText(afterClickText)) {
    const okClicked = await clickPeopleSoftOkIfPresent(page);
    return {
      status: "ghost_outline_unavailable",
      note: okClicked
        ? "No Course Outline page message; clicked OK"
        : "No Course Outline page message; OK button not found",
    };
  }

  const download = await downloadPromise;
  if (download) {
    const outlineFile = path.join(courseFolder, safeName(download.suggestedFilename(), `${courseCode}_outline.pdf`));
    await download.saveAs(outlineFile);
    return { status: "downloaded_outline", outlineFile, note: "downloaded file" };
  }

  const popup = await popupPromise;
  if (popup) {
    await waitPS(popup);
    const ctx = await outlineCtx(popup);
    const payload = await extractOutlinePayload(ctx);
    const text = outlineFieldsToText(payload.fields);
    const html = await ctx.content().catch(async () => await popup.content().catch(() => ""));
    const outlineText = path.join(courseFolder, `${safeName(courseCode)}_course_outline_full_text.txt`);
    const outlineHtml = path.join(courseFolder, `${safeName(courseCode)}_course_outline_page.html`);
    const outlineJson = path.join(courseFolder, `${safeName(courseCode)}_course_outline_fields.json`);
    await fs.writeFile(outlineText, text, "utf8");
    await fs.writeFile(outlineHtml, html, "utf8");
    await fs.writeFile(outlineJson, JSON.stringify(payload.fields, null, 2), "utf8");
    await popup.close().catch(() => {});
    return { status: "saved_outline", outlineText, outlineHtml, outlineJson, note: "popup" };
  }

  await waitPS(page);
  const ctx = await outlineCtx(page);
  const payload = await extractOutlinePayload(ctx);
  const text = outlineFieldsToText(payload.fields);
  const html = await ctx.content().catch(async () => await page.content().catch(() => ""));
  const outlineText = path.join(courseFolder, `${safeName(courseCode)}_course_outline_full_text.txt`);
  const outlineHtml = path.join(courseFolder, `${safeName(courseCode)}_course_outline_page.html`);
  const outlineJson = path.join(courseFolder, `${safeName(courseCode)}_course_outline_fields.json`);
  await fs.writeFile(outlineText, text, "utf8");
  await fs.writeFile(outlineHtml, html, "utf8");
  await fs.writeFile(outlineJson, JSON.stringify(payload.fields, null, 2), "utf8");
  await returnFromOutlineToDetail(page);
  return { status: "saved_outline", outlineText, outlineHtml, outlineJson, note: "same page" };
}

async function findTermsAction(page) {
  const ctx = await activeCtx(page);
  const candidates = await ctx.locator("a").evaluateAll((links) =>
    links
      .map((a) => ({ id: a.id, text: (a.textContent || "").replace(/\s+/g, " ").trim() }))
      .filter((x) =>
        x.id &&
        (/查看课程时段|课程时段|View Class|Class Sections|Course Offering|Course Schedule/i.test(x.text))
      )
  ).catch(() => []);
  return candidates[0]?.id || null;
}

async function expandPastTermsIfPresent(page) {
  const ctx = await activeCtx(page);
  const ids = await ctx.locator("a").evaluateAll((links) =>
    links
      .map((a) => ({ id: a.id, text: (a.textContent || "").replace(/\s+/g, " ").trim() }))
      .filter((x) => x.id && /过去|以往|previous|past|history|展开|expand/i.test(x.text))
      .map((x) => x.id)
  ).catch(() => []);
  for (const id of ids.slice(0, 10)) {
    await submitAction(page, id).catch(() => {});
  }
}

async function clickTermsAndSave(page, courseFolder, courseCode) {
  const actionId = await findTermsAction(page);
  if (!actionId) return { termsStatus: "no_terms_button" };

  const dialogPromise = page.waitForEvent("dialog", { timeout: 8000 }).catch(() => null);
  await submitAction(page, actionId);
  const dialog = await dialogPromise;
  if (dialog) {
    const message = dialog.message();
    await dialog.accept().catch(() => {});
    await waitPS(page);
    const termsText = path.join(courseFolder, `${safeName(courseCode)}_all_offered_terms_dialog.txt`);
    await fs.writeFile(termsText, message, "utf8");
    return { termsStatus: "terms_dialog", termsText, note: message };
  }

  await expandPastTermsIfPresent(page);
  const ctx = await activeCtx(page).catch(() => page);
  const text = await ctx.locator("body").innerText().catch(() => "");
  const html = await page.content().catch(() => "");
  const termsText = path.join(courseFolder, `${safeName(courseCode)}_all_offered_terms_full_text.txt`);
  const termsHtml = path.join(courseFolder, `${safeName(courseCode)}_all_offered_terms_page.html`);
  await fs.writeFile(termsText, text, "utf8");
  await fs.writeFile(termsHtml, html, "utf8");
  return { termsStatus: "saved_terms", termsText, termsHtml };
}

async function main() {
  await fs.mkdir(ROOT, { recursive: true });
  await fs.mkdir(PROFILE_DIR, { recursive: true });
  const manifestDir = path.join(ROOT, "_manifests");
  await fs.mkdir(manifestDir, { recursive: true });

  const context = await chromium.launchPersistentContext(PROFILE_DIR, {
    headless: false,
    acceptDownloads: true,
    viewport: { width: 1440, height: 950 },
  });
  const page = context.pages()[0] || (await context.newPage());
  await page.goto(START_URL);

  const rl = readline.createInterface({ input, output });
  await rl.question(
    "Log in to SIS manually, open Course Management > Course Catalog > Browse Course Catalog, make sure A-Z and Expand All are visible, then press Enter here..."
  );
  if (!RESUME_FROM_COURSE) {
    RESUME_FROM_COURSE = (
      await rl.question("Resume from course code? Press Enter for normal run, or type the first course to crawl, e.g. ECE4011: ")
    )
      .trim()
      .toUpperCase();
  }
  rl.close();

  const seen = new Set();
  let resumeReached = !RESUME_FROM_COURSE;
  if (RESUME_FROM_COURSE) console.log(`Resume mode: will skip courses before ${RESUME_FROM_COURSE}.`);
  for (const letter of LETTERS) {
    console.log(`\n=== Letter ${letter} ===`);
    await clickLetter(page, letter);
    await expandAll(page);
    const courses = await extractVisibleCourses(page);
    console.log(`Visible courses: ${courses.length}`);

    const letterDir = path.join(ROOT, `letter_${letter}`);
    const letterCoursesDir = path.join(letterDir, "courses");
    const letterManifest = path.join(manifestDir, `manifest_letter_${letter}.csv`);
    await fs.mkdir(letterDir, { recursive: true });
    await fs.mkdir(letterCoursesDir, { recursive: true });
    if (courses.length === 0) {
      const ctx = await activeCtx(page).catch(() => page);
      const debugText = await ctx.locator("body").innerText().catch(() => "");
      const debugHtml = await ctx.content().catch(async () => await page.content().catch(() => ""));
      await fs.writeFile(path.join(letterDir, `debug_zero_courses_letter_${letter}.txt`), debugText, "utf8");
      await fs.writeFile(path.join(letterDir, `debug_zero_courses_letter_${letter}.html`), debugHtml, "utf8");
      throw new Error(
        `Letter ${letter} has 0 visible courses. Saved debug_zero_courses_letter_${letter}.txt/html. Make sure the ${letter} page is selected and expanded.`
      );
    }
    await fs.writeFile(path.join(letterDir, `course_list_letter_${letter}.json`), JSON.stringify(courses, null, 2), "utf8");

    for (const course of courses) {
      if (!resumeReached) {
        if (course.courseCode === RESUME_FROM_COURSE) {
          resumeReached = true;
          console.log(`  ${course.courseCode} ${course.courseTitle} -- RESUME START`);
        } else {
          console.log(`  ${course.courseCode} ${course.courseTitle} -- SKIP before ${RESUME_FROM_COURSE}`);
          continue;
        }
      }
      const key = `${course.subject}|${course.courseNumber}|${course.courseTitle}`;
      if (seen.has(key)) continue;
      seen.add(key);
      if (SKIP_COURSE_CODES.has(course.courseCode)) {
        console.log(`  ${course.courseCode} ${course.courseTitle} -- SKIP`);
        continue;
      }

      console.log(`  ${course.courseCode} ${course.courseTitle}`);
      const courseFolder = path.join(letterCoursesDir, safeName(`${course.courseCode}_${course.courseTitle}`));
      await fs.mkdir(courseFolder, { recursive: true });

      try {
        await gotoCourseDetail(page, course);
        const detail = await extractDetail(page);
        const detailJson = path.join(courseFolder, "detail.json");
        await fs.writeFile(detailJson, JSON.stringify({ ...course, ...detail }, null, 2), "utf8");

        const outline = await clickOutlineAndSave(page, courseFolder, course.courseCode);
        if (!outline.status.startsWith("saved") && !outline.status.startsWith("downloaded")) {
          const row = {
            letter,
            subject: course.subject,
            course_code: course.courseCode,
            course_number: course.courseNumber,
            course_title: course.courseTitle,
            status: outline.status,
            outline_json: "",
            outline_text: "",
            outline_html: "",
            terms_status: "",
            terms_text: "",
            terms_html: "",
            detail_json: detailJson,
            note: outline.note,
          };
          await appendCsv(letterManifest, row);
          await returnToCatalog(page);
          continue;
        }

        const terms = await clickTermsAndSave(page, courseFolder, course.courseCode);
        const row = {
          letter,
          subject: course.subject,
          course_code: course.courseCode,
          course_number: course.courseNumber,
          course_title: course.courseTitle,
          status: outline.status,
          outline_json: outline.outlineJson || "",
          outline_text: outline.outlineText || outline.outlineFile || "",
          outline_html: outline.outlineHtml || "",
          terms_status: terms.termsStatus || "",
          terms_text: terms.termsText || "",
          terms_html: terms.termsHtml || "",
          detail_json: detailJson,
          note: [outline.note, terms.note].filter(Boolean).join(" | "),
        };
        await appendCsv(letterManifest, row);
        await returnToCatalog(page);
      } catch (err) {
        console.error(`    ERROR: ${err.message}`);
        const row = {
          letter,
          subject: course.subject,
          course_code: course.courseCode,
          course_number: course.courseNumber,
          course_title: course.courseTitle,
          status: "error",
          outline_json: "",
          outline_text: "",
          outline_html: "",
          terms_status: "",
          terms_text: "",
          terms_html: "",
          detail_json: "",
          note: err.message,
        };
        await appendCsv(letterManifest, row);
        await returnToCatalog(page).catch(async () => {
          await page.goBack().catch(() => {});
          await waitPS(page).catch(() => {});
        });
      }
    }
  }

  if (!resumeReached) {
    console.warn(`WARNING: resume course ${RESUME_FROM_COURSE} was not found in selected letters: ${LETTERS.join(",")}`);
  }
  console.log(`Done. Letter manifests are in: ${manifestDir}`);
  await context.close();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
