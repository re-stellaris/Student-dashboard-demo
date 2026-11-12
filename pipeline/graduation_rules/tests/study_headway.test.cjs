const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const rules = require('../students-interface/study_headway_rules.js');
const engine = require('../students-interface/study_headway_engine.js');
const get = (key, year, stream) => {
  const v = engine.variantFor(rules.programmes[key], year);
  return [v, stream ? v.paths.find(p => p.id === stream) : v.paths[0]];
};
const has = (groups, code) => groups.some(g => g.options.flat().includes(code));
const allPassed = (v, p) => new Map([...p.components.flatMap(c => c.groups).flatMap(g => g.options[0]), ...p.pools.flatMap(q => q.groups).flatMap(g => g.options[0])].map(c => [c, v.courseUnits[c] || 0]).filter(([,u]) => u > 0));

test('only the eleven revised majors, 39 source variants and 80 paths are overridden', () => {
  assert.equal(Object.keys(rules.programmes).length, 11);
  const variants = Object.values(rules.programmes).flatMap(p => p.variants);
  assert.equal(variants.length, 39); assert.equal(variants.flatMap(v => v.paths).length, 80);
  for (const key of ['mse','artificial_intelligence','financial_engineering','economics','translation','clinical_medicine']) assert.equal(rules.programmes[key], undefined);
  for (const p of Object.values(rules.programmes)) {
    for (let y = 2021; y <= 2027; y++) assert.ok(p.variants.filter(v => y >= v.from && (v.to == null || y <= v.to)).length <= 1);
    for (const v of p.variants) {
      assert.match(v.source.sha256, /^[a-f0-9]{64}$/);
      for (const r of v.paths) {
        assert.equal(r.total, r.electiveUnits + r.components.reduce((s,c) => s + c.minUnits, 0));
        for (const c of r.components) { assert.ok(c.groups.length); assert.ok(c.minUnits > 0); }
        // Threshold pools must be disjoint: a course cannot satisfy two independent quotas.
        const quota = r.pools.filter(p => p.minUnits || p.minCourses);
        for (let i = 0; i < quota.length; i++) for (let j = i + 1; j < quota.length; j++) {
          const a = new Set(quota[i].groups.flatMap(g => g.options.flat()));
          assert.deepEqual(quota[j].groups.flatMap(g => g.options.flat()).filter(x => a.has(x)), []);
        }
      }
    }
  }
});
test('all populated portfolios satisfy supported paths, except source-blocked conducting totals', () => {
  for (const programme of Object.values(rules.programmes)) for (const v of programme.variants) for (const p of v.paths) {
    const r = engine.evaluate(v, p, allPassed(v, p));
    assert.equal(r.complete, !p.issues.some(i => i.blocking), `${v.id}/${p.id}: ${JSON.stringify(r.components.filter(c => !c.complete))}`);
  }
});
test('2026 SDS denominators and BIO1008 move do not change earlier cohorts', () => {
  for (const key of ['statistics','dsbdt','cse']) {
    const [old, op] = get(key,2025), [v,p] = get(key,2026);
    assert.equal(op.total,70); assert.equal(p.total,key === 'cse' ? 70 : 71);
    assert.ok(has(op.components[0].groups,'BIO1008')); assert.ok(!has(p.components[0].groups,'BIO1008'));
    assert.ok(has(p.components[0].groups,'DDA1000')); assert.ok(has(p.pools.flatMap(q=>q.groups),'BIO1008'));
    assert.ok(v.from > old.from);
  }
});
test('ECE computer-engineering requirement changes at 2024, not at 2025', () => {
  const [,old] = get('ece',2023,'Computer Engineering'), [,p] = get('ece',2024,'Computer Engineering');
  assert.equal(old.components[1].minUnits,29); assert.equal(old.electiveUnits,21);
  assert.equal(p.components[1].minUnits,26); assert.equal(p.electiveUnits,24);
  assert.ok(has(old.components[1].groups,'CSC3050')); assert.ok(!has(p.components[1].groups,'CSC3050'));
  assert.ok(has(p.pools[0].groups,'CSC3050'));
  assert.ok(has(p.components[1].groups,'ECE3070'));
});
test('whole programming alternatives cannot be mixed', () => {
  const [v,p] = get('cse',2026), earned = allPassed(v,p);
  earned.delete('CSC1001'); earned.delete('CSC1002'); earned.set('CSC1001',3); earned.set('CSC1004',1);
  assert.equal(engine.evaluate(v,p,earned).complete,false);
  earned.set('CSC1002',1); assert.equal(engine.evaluate(v,p,earned).complete,true);
});
test('statistics 2026 explicitly permits non-honours alternatives; mathematics does not', () => {
  const [v,p] = get('statistics',2026), earned = allPassed(v,p);
  earned.delete('STA2001H');earned.delete('STA2002H');earned.set('STA2001',3);earned.set('STA2002',3);
  assert.equal(engine.evaluate(v,p,earned).complete,true);
  const [m,mp] = get('math',2025,'Pure Mathematics'), me = allPassed(m,mp);
  me.delete('STA2001H');me.set('STA2001',3); assert.equal(engine.evaluate(m,mp,me).complete,false);
});
test('chemistry extended labs are alternatives for 2025, compulsory for 2026', () => {
  const [,old] = get('chemistry',2025), [v,p] = get('chemistry',2026);
  assert.ok(has(old.components[1].groups,'CHM2118'));
  assert.ok(!has(p.components[1].groups,'CHM2118'));
  assert.ok(has(p.components[1].groups,'CHM2118E'));
  assert.ok(!has(p.components[1].groups,'BIO2004'));assert.ok(has(p.pools[1].groups,'BIO2004'));
  const earned = allPassed(v,p);earned.delete('CHM2118E');earned.set('CHM2118',2);
  assert.equal(engine.evaluate(v,p,earned).complete,false);
});
test('old statistics needs three-category breadth and depth; 2025 drops breadth', () => {
  const [v,p] = get('statistics',2023), e = allPassed(v,p);
  p.pools.slice(2).flatMap(q=>q.groups).flatMap(g=>g.options.flat()).forEach(c=>e.delete(c));
  assert.equal(engine.evaluate(v,p,e).complete,false);
  const three=allPassed(v,p);p.pools.slice(3).flatMap(q=>q.groups).flatMap(g=>g.options.flat()).forEach(c=>three.delete(c));
  const result=engine.evaluate(v,p,three);assert.equal(result.checks.find(c=>c.type==='breadth').complete,true);
  const [,n] = get('statistics',2025);assert.deepEqual(n.constraints.map(c=>c.type),['depth']);
});
test('DS low-level electives are capped; repeated aliases and pool overlap earn credit once', () => {
  const [v,p] = get('dsbdt',2026), e = allPassed(v,p); const r = engine.evaluate(v,p,e);
  assert.ok(r.excluded.length > 0);
  const before = r.electiveUnits;e.set('STA4001H',3);e.set('STA4001',3);
  const added = engine.evaluate(v,p,e).electiveUnits;
  assert.ok(added - before <= 3);
  assert.ok(r.electiveUnits < r.pools.reduce((s,q)=>s+q.units,0));
});
test('course basket, failed/in-progress records and missing credit values do not grant credit', () => {
  const pass = g => ['A','P'].includes(g);
  const r=engine.earnedRecords([{code:'CSC 1001',grade:'A',credit:3},{code:'CSC1001',grade:'P',credit:3},{code:'X1001',grade:'F',credit:3},{code:'X1002',grade:'IP',credit:3},{code:'X1003',grade:'A'},{code:'BIO1008',grade:'P'}],{BIO1008:3},pass);
  assert.equal(r.earned.size,2);assert.equal(r.earned.get('CSC1001'),3);assert.deepEqual(r.unknown,['X1003']);
  assert.equal(engine.earnedRecords([],{CSC1001:3},pass).earned.size,0);
});
test('music 2025/26 conducting remains needs-review even with all courses', () => {
  for (const year of [2025,2026]) { const [v,p]=get('music_performance',year,'Conducting');const r=engine.evaluate(v,p,allPassed(v,p));assert.equal(r.total,69);assert.equal(r.complete,false);assert.equal(r.blocking[0].code,'conflicting_total'); }
  assert.equal(engine.variantFor(rules.programmes.music_performance,2022),null);
});
test('optional stream declarations do not become mandatory graduation conditions', () => {
  const [v,p]=get('statistics',2026),e=allPassed(v,p);
  assert.equal(engine.evaluate(v,p,e).declaration,null);
  assert.equal(engine.evaluate(v,p,e,p.optionalStreams[0].id).declaration.complete,true);
});
test('dashboard and standalone browser scripts parse; new assets are loaded before inline code', () => {
  const html=fs.readFileSync(path.join(__dirname,'../students-interface/student_dashboard_index.html'),'utf8');
  for (const m of html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)) new vm.Script(m[1]);
  for (const file of ['study_headway_engine.js','study_headway_view.js','study_headway_rules.js']) {new vm.Script(fs.readFileSync(path.join(__dirname,'../students-interface',file),'utf8'));assert.ok(html.includes(`/students-interface/${file}`));}
  assert.ok(html.indexOf('/students-interface/study_headway_rules.js')<html.indexOf('const REMOTE_API_BASE'));
});
test('elective rows identify counted courses, mandatory overlap and low-level exclusions', () => {
  const [v,p]=get('dsbdt',2026),e=allPassed(v,p),r=engine.evaluate(v,p,e);
  const statuses=r.pools.flatMap(q=>q.courseStatuses);
  assert.ok(statuses.some(s=>s.counted && s.completed && s.units>0));
  for (const excluded of r.excluded) assert.ok(statuses.some(s=>s.code===excluded.code && s.completed && !s.counted && s.reason==='level_limit'));
  const empty=engine.evaluate(v,p,new Map());
  assert.ok(empty.pools.every(q=>q.courseStatuses.every(s=>!s.completed && s.reason==='not_completed')));
  const [cv,cp]=get('chemistry',2026),cr=engine.evaluate(cv,cp,new Map());
  assert.deepEqual(cr.pools.map(q=>q.unitsShort),[2,6,9]);
});
test('energy elective view exposes its actual 18-unit target, full pool and completion statuses', () => {
  const [v,p]=get('nese',2023,'New Energy Science');
  const mandatory=new Set(p.components.flatMap(c=>c.groups).flatMap(g=>g.options.flat()));
  const code=p.pools[0].groups.flatMap(g=>g.options.flat()).find(c=>!mandatory.has(c) && v.courseUnits[c]);
  const context={window:{StudyHeadwayEngine:engine}};vm.createContext(context);
  vm.runInContext(fs.readFileSync(path.join(__dirname,'../students-interface/study_headway_view.js'),'utf8'),context);
  const el={innerHTML:'',querySelector:()=>null};
  context.window.renderUpdatedStudyHeadway(el,{
    programme:rules.programmes.nese,major:'nese',year:2023,records:[{code,credit:v.courseUnits[code],grade:'P'}],
    selections:{['nese:'+v.id]:{path:p.id}},onSelection:()=>{},isPassing:g=>g==='P',esc:s=>String(s??''),
    courseLink:c=>`<a>${c}</a>`,progressBlock:(title,done,total,body)=>`${title} ${done}/${total}${body}`,ucoreHtml:()=>'',resize:()=>{}
  });
  assert.match(el.innerHTML,/Elective requirements/);
  assert.match(el.innerHTML,/\/18 elective units/);assert.doesNotMatch(el.innerHTML,/30 elective units/);
  assert.match(el.innerHTML,/headway-course done/);assert.match(el.innerHTML,/✓ completed/);assert.match(el.innerHTML,/to take/);
  assert.match(el.innerHTML,/Still need \d+ eligible elective units/);
  for(const group of p.pools[0].groups) for(const c of group.options.flat()) assert.ok(el.innerHTML.includes(`<a>${c}</a>`));
});
