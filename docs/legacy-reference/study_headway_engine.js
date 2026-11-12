/* Source-based major progress. Deliberately independent of the course basket. */
(function (root) {
  'use strict';
  const normalize = code => String(code || '').toUpperCase().replace(/\s+/g, '');
  const sum = values => values.reduce((a, b) => a + b, 0);
  const codes = group => group.options.flat();
  function variantFor(programme, year) {
    return programme?.variants.find(v => Number(year) >= v.from && (v.to == null || Number(year) <= v.to)) || null;
  }
  function earnedRecords(records, units, isPassing) {
    const earned = new Map(), unknown = new Set();
    for (const record of records || []) {
      if (!isPassing(record.grade)) continue;
      const code = normalize(record.code);
      if (!code) continue;
      const supplied = record.credit;
      const value = supplied != null && supplied !== '' ? Number(supplied) : Number(units[code]);
      if (!Number.isFinite(value) || value <= 0) { unknown.add(code); continue; }
      earned.set(code, Math.max(earned.get(code) || 0, value));
      unknown.delete(code);
    }
    return {earned, unknown: [...unknown].filter(c => !earned.has(c))};
  }
  function evaluate(variant, path, earned, declarationId = '') {
    // Official code changes and printed single-course alternatives are one credit identity.
    const parent = new Map();
    const find = c => { if (!parent.has(c)) parent.set(c, c); if (parent.get(c) !== c) parent.set(c, find(parent.get(c))); return parent.get(c); };
    const join = (a, b) => { parent.set(find(b), find(a)); };
    const allGroups = [...path.components.flatMap(c => c.groups), ...path.pools.flatMap(p => p.groups)];
    for (const pair of variant.codeAliases || []) join(pair[0], pair[1]);
    for (const g of allGroups) if (g.options.every(a => a.length === 1)) g.options.slice(1).forEach(a => join(g.options[0][0], a[0]));
    const available = new Map();
    for (const [code, units] of earned) {
      const id = find(normalize(code));
      if (!available.has(id) || available.get(id).units < units) available.set(id, {id, code: normalize(code), units});
    }
    const used = new Set();
    const components = path.components.map(component => {
      const groups = component.groups.map(g => {
        const options = g.options.map(option => {
          const ids = [...new Set(option.map(find))];
          const matches = ids.filter(id => available.has(id) && !used.has(id));
          return {ids, matches, complete: matches.length === ids.length, units: sum(matches.map(id => available.get(id).units))};
        }).sort((a, b) => Number(b.complete) - Number(a.complete) || b.units - a.units);
        const chosen = options[0];
        chosen.matches.forEach(id => used.add(id));
        return {group: g, complete: chosen.complete, units: chosen.units};
      });
      const units = sum(groups.map(g => g.units));
      return {...component, groups, units, complete: groups.every(g => g.complete) && units >= component.minUnits};
    });
    // Reserve every mandatory alternative: taking both alternatives cannot create an elective.
    path.components.flatMap(c => c.groups).flatMap(codes).forEach(c => used.add(find(c)));
    const poolIds = path.pools.map(p => new Set(p.groups.flatMap(codes).map(find)));
    const candidates = [...available.values()].filter(c => !used.has(c.id) && poolIds.some(ids => ids.has(c.id)));
    const statsFor = list => path.pools.map((p, index) => {
      const matches = list.filter(c => poolIds[index].has(c.id));
      return {...p, units: sum(matches.map(c => c.units)), count: matches.length, codes: matches.map(c => c.code)};
    });
    const checksFor = stats => (path.constraints || []).filter(c => c.type !== 'max_level_count').map(c => {
      const pools = stats.filter(p => (c.pools || []).includes(p.id));
      const value = c.type === 'course_count' ? new Set(stats.flatMap(p => p.codes)).size :
        c.type === 'breadth' ? pools.filter(p => p.count > 0).length : Math.max(0, ...pools.map(p => p.count));
      return {...c, value, complete: value >= c.min};
    });
    let counted = candidates;
    for (const limit of (path.constraints || []).filter(c => c.type === 'max_level_count')) {
      const isLow = c => { const level = Number(c.code.match(/\d{4}/)?.[0]); return level >= limit.atLeast && level < limit.below; };
      const low = counted.filter(isLow), high = counted.filter(c => !isLow(c));
      if (low.length <= limit.max) continue;
      // Try the allowed subsets, so a low-level choice cannot falsely fail breadth/depth.
      let best = [], bestScore = -Infinity;
      function choose(start, chosen) {
        if (chosen.length === limit.max) {
          const list = [...high, ...chosen], stats = statsFor(list), checks = checksFor(stats);
          const score = checks.filter(c => c.complete).length * 10000 + Math.min(path.electiveUnits, sum(list.map(c => c.units)));
          if (score > bestScore) { best = list; bestScore = score; }
          return;
        }
        for (let i = start; i < low.length; i++) choose(i + 1, [...chosen, low[i]]);
      }
      choose(0, []); counted = best;
    }
    const countedIds = new Set(counted.map(c => c.id));
    const pools = statsFor(counted).map(p => ({...p,
      unitsShort: Math.max(0, p.minUnits - p.units),
      coursesShort: Math.max(0, p.minCourses - p.count),
      courseStatuses: p.groups.map(group => {
        const ids = [...new Set(codes(group).map(find))];
        const countedMatch = ids.find(id => countedIds.has(id));
        const earnedMatch = countedMatch || ids.find(id => available.has(id));
        const match = earnedMatch ? available.get(earnedMatch) : null;
        return {group, counted: !!countedMatch, completed: !!match, code: match?.code || null,
          units: match?.units || 0,
          reason: countedMatch ? 'counted' : match ? (used.has(earnedMatch) ? 'mandatory' : 'level_limit') : 'not_completed'};
      })
    })), checks = checksFor(pools);
    const electiveUnits = sum(counted.map(c => c.units));
    const excluded = candidates.filter(c => !counted.includes(c));
    const blocking = path.issues.filter(i => i.blocking);
    const complete = !blocking.length && components.every(c => c.complete) && electiveUnits >= path.electiveUnits &&
      pools.every(p => p.units >= p.minUnits && p.count >= p.minCourses) && checks.every(c => c.complete);
    const done = sum(components.map(c => Math.min(c.minUnits, c.units))) + Math.min(path.electiveUnits, electiveUnits);
    const declaration = (path.optionalStreams || []).find(s => s.id === declarationId);
    let declarationResult = null;
    if (declaration) {
      const requirements = (declaration.requirements || [declaration]).map(req => {
        const ids = new Set(req.groups.flatMap(codes).map(find));
        const count = [...available.keys()].filter(id => ids.has(id)).length;
        return {count, min: req.minCourses, complete: count >= req.minCourses};
      });
      declarationResult = {...declaration, requirements, complete: requirements.every(r => r.complete)};
    }
    return {complete, done, total: path.total, components, pools, checks, electiveUnits, excluded, blocking, declaration: declarationResult};
  }
  const api = {normalize, variantFor, earnedRecords, evaluate};
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.StudyHeadwayEngine = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
