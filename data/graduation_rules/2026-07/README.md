# Study Headway：2026 年 7 月方案复核与代码更新

分析日期：2026-09-25。GitHub 基线：`cfe0bddfeb35a39d49c6a9ee8ef0dc9ffe2182b8`。

选修界面补充：恢复原手动设计的逐科已完成/待修状态，并显示选修总缺口、各组最低学分/门数及未计分原因。新能源 2023 两方向均使用 18 学分选修池。回归测试已扩展为 15 项，包含课程清单与计分状态的界面验证。

## 范围与执行状态

本地目录共 80 份 PDF。按用户要求排除材料科学与工程、SME、MED、HSS，共 35 份。
其余 45 份中，37 份相对上次快照有变化或新增，8 份 SHA-256 相同。
本次更新 11 个主修专业，覆盖 39 份入学年份方案、80 个专业/年份/方向组合；
其中包含化学与新能源各一份未变化的旧年级方案，以保留这些主修的完整年份分支。
AI、金融工程和 U-core 的 6 份未变化文件继续使用现有实现。排除专业的规则不改动。
`scope-manifest.json` 中 `oldId` 仅表示与上次提取内容哈希相同，不代表新旧文件名相同。

本次用户明确授权分析后直接修改代码并更新 GitHub；不把这项工程授权记为教务审核通过。
存在原文矛盾的条件仍保留为待确认，禁止自动判定完成。

## 主要修订

| 专业 | 已落实的要求 |
| --- | --- |
| 作曲与作曲技术理论 | 2026 起学院课程 11、必修 59，总计 70；旧版学院课程 51、必修 19，分别按适用年份保留。 |
| 音乐学 | 2022/23 为 49+19；2024/25 为 27+41；2026 起 11+57，总计 68。支持原文列出的旧、新课号对应。 |
| 音乐表演 | 必须选择七个方向之一；各方向分别使用原表课程及 69/71 学分要求，不能用 MUS 前缀泛计。2023 指挥的乐器系列必须整组选修。 |
| 数据科学与大数据技术 | 2026 学院课程 23、必修 18、选修 30，总计 71；旧版总计 70。BIO1008 从学院课程移入生命科学池，新增 DDA1000。选修低年级课程上限按各版本处理，五个方向声明可选。 |
| 统计学 | 2026 总计 71（23+18+30）；2025 总计 70（25+18+27）。旧版需 9 科、至少覆盖五类中的三类，以及四个主领域之一至少三科；补充选修是可选类别，并非强制类别。2025 起按深度三科、可选方向声明四科处理，不沿用旧版广度条件。 |
| 计算机科学与技术 | 2023 为 25+27+18，六科选修至少四科 A 类；2024/25 为 25+20+25，A 类至少 16 学分；2026 为 23+20+27，A 类至少 18 学分，总计仍为 70。编程替代须完整课程对。 |
| 化学 | 总计 72，学院课程 22、必修 33、选修 17，选修 A/B/C 分别至少 2/6/9。2026 BIO2004 移入 B 池，CHM2118E/2228E/3418E 成为必修；2024/25 普通/延伸实验课按脚注允许替代。 |
| 数学与应用数学 | 总计 70（22+27+21），2023 三方向，2024 起四方向；按各方向更新完整选修池。微积分和编程组合必须整组满足；不擅自把 STA2001 当成 STA2001H。 |
| 新能源科学与工程 | 旧版两个方向均 22+32+18；2024 起新能源科学 22+26+24、智能能源工程 22+32+18。池内主题组是分类，不增设原文没有的分组最低学分。 |
| 物理学 | 总计 72（22+38+12）；2024 起分物理与应用物理/人工智能方向。其他合适课程最多 6 学分须专业批准，不能按前缀自动替代。 |
| 电子与计算机工程 | 2022/23 计算机工程 22+29+21；2024 起 22+26+24，必修课程清单同时重组。2025 起加入微电子方向 22+32+18。A/B 池按方向分别计额。 |

## 原文问题与处理

1. 音乐表演 2025、2026 指挥方向正文要求 71，但表格各项合计 69：保留表格进度，明确显示“Rule needs confirmation”，即使课程学分满足也不判定完成。
2. 音乐表演 2023 指挥第二乐器系列在课程表合计 5 学分，而主修选修栏要求 7；显示矛盾说明并保留 7 学分门槛，不补造 2 学分。钢琴系列合计 7，可按正常规则计算。
3. ECE 2025 起英文第 1 页出现 `5023ECE3040` 缺逗号；中文第 5 页确认是 AIR5023 与 ECE3040 两科。按此分开，并记录证据。
4. 课号变更仅采用源文件明确说明的对应关系（如 STA2003/STA2001H、STA2004/STA2002H、AIR 旧新代码、音乐学旧新代码、CHM2001/CHM2310）。不把任意荣誉课或同前缀课自动当替代课。
5. 科目学分优先取已通过课程记录；缺失时使用该份方案的课程表，仍未知则不计分并提示。音乐历史学分变化以成绩记录为准。

## 计算与验证

新引擎只计算通过的成绩记录。课表篮子中的计划课、未通过课、在修课不算完成。
同一课号重复记录、官方改名及列明的单科替代不重复计入主修学分；跨选修方向出现的同一科只计一次总学分。
学分达到总额还必须满足必修、选修类别、门数及方向条件。无对应年份文件时显示未覆盖，不套用其他年份。
方向选择按主修和方案版本保存。可选方向声明单独展示，不强加为毕业条件。
研究生选修的选课批准、专业批准的外部替代需以正式记录为依据；本次不会推断批准。

验证：`npm run test:headway`；另用隔离浏览器验证年份/方向切换、持久化、可选方向、计划课与已通过课程区别，以及排除专业仍走原实现。
规则测试遍历全部 80 个组合，并针对课程对混搭、实验课版本、统计广度/深度、选修门数上限和原文冲突设置反例。
U-core 源文件未变化，复用原有规则；其教务规则和既有 GPA/成绩导入逻辑不在此次改写范围。

## 文件与复现

- `source_documents.json`：原 PDF 路径、SHA-256、主规则页、逐科学分及证据页、课号变更依据。
- `analysis-index.json`：逐文件适用年份、方向、学分及问题索引。
- `students-interface/study_headway_rules.js`：生成的浏览器规则。
- `tools/extract_headway_sources.py --root <PDF目录>`：核验哈希后重新提取当前已复核版本（需要 pdfplumber）。
- `python tools/compile_headway_rules.py`：从证据快照重建 JS、索引和本地可读的 `rules.json`。
- `npm run test:headway`：运行无需网络的回归测试。

## 逐方案适用范围

| ID | 主修 | 适用入学年份 | 方向与学分 | 源文件 |
| --- | --- | --- | --- | --- |
| u01 | Music Composition and Theory | 2022–2023 | 主修: 70 | MUS/作曲与作曲技术理论/study_scheme_-_music_composition_and_theory_2022-23_and_2023-24_3rd2026_20260720.pdf |
| u02 | Music Composition and Theory | 2024 | 主修: 70 | MUS/作曲与作曲技术理论/study_scheme_-_music_composition_and_theory_2024-25_3rd2026_20260720.pdf |
| u03 | Music Composition and Theory | 2025 | 主修: 70 | MUS/作曲与作曲技术理论/study_scheme_-_music_composition_and_theory_2025-26_3rd2026_20260720.pdf |
| u04 | Music Composition and Theory | 2026 起 | 主修: 70 | MUS/作曲与作曲技术理论/study_scheme_-_music_composition_and_theory_2026-27_and_thereafter_3rd2026_20260720.pdf |
| u05 | Musicology | 2022–2023 | 主修: 68 | MUS/音乐学/study_scheme_-_musicology_2022-23_and_2023-24_3rd2026_20260720.pdf |
| u06 | Musicology | 2024–2025 | 主修: 68 | MUS/音乐学/study_scheme_-_musicology_2024-25_and_2025-26_3rd2026_20260720.pdf |
| u07 | Musicology | 2026 起 | 主修: 68 | MUS/音乐学/study_scheme_-_musicology_2026-27_and_thereafter_3rd2026_20260720.pdf |
| u08 | Music Performance | 2023 | Voice: 71；Strings: 71；Woodwinds: 71；Brass: 71；Piano: 69；Conducting: 71；Percussion: 71 | MUS/音乐表演/study_scheme_-_music_performance_2023-24_3rd2026_20260720.pdf |
| u09 | Music Performance | 2024 | Voice: 71；Strings: 71；Woodwinds: 71；Brass: 71；Piano: 69；Conducting: 71；Percussion: 71 | MUS/音乐表演/study_scheme_-_music_performance_2024-25_3rd2026_20260720.pdf |
| u10 | Music Performance | 2025 | Voice: 71；Strings: 71；Woodwinds: 71；Brass: 71；Piano: 69；Conducting: 69（待确认）；Percussion: 71 | MUS/音乐表演/study_scheme_-_music_performance_2025-26_3rd2026_20260720.pdf |
| u11 | Music Performance | 2026 起 | Voice: 71；Strings: 71；Woodwinds: 71；Brass: 71；Piano: 69；Conducting: 69（待确认）；Percussion: 71 | MUS/音乐表演/study_scheme_-_music_performance_2026-27_and_thereafter_3rd2026_20260720.pdf |
| u13 | Data Science and Big Data Technology | 2023 | 主修: 70 | SDS/数据科学与大数据技术/study_scheme_-_dsbdt_2023-24_3rd2026_20260720.pdf |
| u14 | Data Science and Big Data Technology | 2024 | 主修: 70 | SDS/数据科学与大数据技术/study_scheme_-_dsbdt_2024-25_3rd2026_20260720.pdf |
| u15 | Data Science and Big Data Technology | 2025 | 主修: 70 | SDS/数据科学与大数据技术/study_scheme_-_dsbdt_2025-26_3rd2026_20260720.pdf |
| u16 | Data Science and Big Data Technology | 2026 起 | 主修: 71 | SDS/数据科学与大数据技术/study_scheme_-_dsbdt_2026-27_and_thereafter_3rd2026_20260720.pdf |
| u17 | Statistics | 2022–2023 | 主修: 70 | SDS/统计学/study_scheme_-_stat_2022-23_and_2023-24_3rd2026_20260720.pdf |
| u18 | Statistics | 2024 | 主修: 70 | SDS/统计学/study_scheme_-_stat_2024-25_3rd2026_20260720.pdf |
| u19 | Statistics | 2025 | 主修: 70 | SDS/统计学/study_scheme_-_stat_2025-26_3rd2026_20260720.pdf |
| u20 | Statistics | 2026 起 | 主修: 71 | SDS/统计学/study_scheme_-_stat_2026-27_and_thereafter_3rd2026_20260720.pdf |
| u21 | Computer Science and Engineering | 2023 | 主修: 70 | SDS/计算机科学与技术/study_scheme_-_cse_2023-24_3rd2026_20260720.pdf |
| u22 | Computer Science and Engineering | 2024 | 主修: 70 | SDS/计算机科学与技术/study_scheme_-_cse_2024-25_3rd2026_20260720.pdf |
| u23 | Computer Science and Engineering | 2025 | 主修: 70 | SDS/计算机科学与技术/study_scheme_-_cse_2025-26_3rd2026_20260720.pdf |
| u24 | Computer Science and Engineering | 2026 起 | 主修: 70 | SDS/计算机科学与技术/study_scheme_-_cse_2026-27_and_thereafter_3rd2026_20260720.pdf |
| u25 | Chemistry | 2022–2023 | 主修: 72 | SSE/化学/Study Scheme - Chemistry_2022-23 and 2023-24_Circular (AB2025_C015).pdf |
| u26 | Chemistry | 2024 | 主修: 72 | SSE/化学/study_scheme_-_chemistry_2024-25_3rd2026_20260720.pdf |
| u27 | Chemistry | 2025 | 主修: 72 | SSE/化学/study_scheme_-_chemistry_2025-26_3rd2026_20260720.pdf |
| u28 | Chemistry | 2026 起 | 主修: 72 | SSE/化学/study_scheme_-_chemistry_2026-27_and_thereafter_3rd2026_20260720.pdf |
| u29 | Mathematics and Applied Mathematics | 2023 | Pure Mathematics: 70；Applied Mathematics: 70；Financial Mathematics: 70 | SSE/数学与应用数学/study_scheme_-_math_2023-24_3rd2026_20260720.pdf |
| u30 | Mathematics and Applied Mathematics | 2024 | Pure Mathematics: 70；Applied Mathematics: 70；Financial Mathematics: 70；Mathematical Modeling and AI: 70 | SSE/数学与应用数学/study_scheme_-_math_2024-25_3rd2026_20260720_0.pdf |
| u31 | Mathematics and Applied Mathematics | 2025 起 | Pure Mathematics: 70；Applied Mathematics: 70；Financial Mathematics: 70；Mathematical Modeling and AI: 70 | SSE/数学与应用数学/study_scheme_-_math_2025-26_and_thereafter_3rd2026_20260720.pdf |
| u32 | New Energy Science and Engineering | 2022–2023 | New Energy Science: 72；New Energy Engineering: 72 | SSE/新能源科学与工程/Study Scheme - ENER_2022-23 and 2023-24_Circular(AB2025_C047).pdf |
| u33 | New Energy Science and Engineering | 2024 | New Energy Science: 72；Intelligent Energy Engineering: 72 | SSE/新能源科学与工程/study_scheme_-_ener_2024-25_3rd2026_20260720.pdf |
| u34 | New Energy Science and Engineering | 2025 起 | New Energy Science: 72；Intelligent Energy Engineering: 72 | SSE/新能源科学与工程/study_scheme_-_ener_2025-26_and_thereafter_3rd2026_20260720.pdf |
| u35 | Physics | 2023 | 主修: 72 | SSE/物理学/study_scheme_-_phy_2023-24_3rd2026_20260720_0.pdf |
| u36 | Physics | 2024 | Physics: 72；Applied Physics and AI: 72 | SSE/物理学/study_scheme_-_phy_2024-25_3rd2026_20260720_0.pdf |
| u37 | Physics | 2025 起 | Physics: 72；Applied Physics and AI: 72 | SSE/物理学/study_scheme_-_phy_2025-26_and_thereafter_3rd2026_20260720.pdf |
| u38 | Electrical and Computer Engineering | 2022–2023 | Electronic Engineering: 72；Computer Engineering: 72 | SSE/电子与计算机工程/study_scheme_-_ece_2022-23_and_2023-24_3rd2026_20260720.pdf |
| u39 | Electrical and Computer Engineering | 2024 | Electronic Engineering: 72；Computer Engineering: 72 | SSE/电子与计算机工程/study_scheme_-_ece_2024-25_3rd2026_20260720.pdf |
| u40 | Electrical and Computer Engineering | 2025 起 | Electronic Engineering: 72；Computer Engineering: 72；Microelectronics Science and Engineering: 72 | SSE/电子与计算机工程/study_scheme_-_ece_2025-26_and_thereafter_3rd2026_20260720.pdf |
