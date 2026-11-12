---
name: evaluate-course-skill-fit
description: Semantically evaluate whether university courses genuinely cultivate job skills using full course titles, descriptions, learning outcomes, syllabi, assessments, and prerequisites. Use when building or auditing job-skill-course recommendations, replacing keyword-only matching, detecting false positives such as ambiguous word senses, and producing evidence-backed labels of direct training, foundational support, or not related.
---

# Evaluate Course-Skill Fit

Evaluate unique `skill × course` pairs with semantic reasoning. Never treat keyword occurrence as sufficient evidence.

## Workflow

1. Load job-skill mappings and course records.
2. Restrict course records to courses with a substantive syllabus.
3. Generate broad candidate pairs with deterministic retrieval. Treat these as candidates, not recommendations.
4. Deduplicate by normalized skill and course code.
5. Run `scripts/prepare_review_batches.py` to create AI-review JSONL batches.
6. Read [references/evaluation-rubric.md](references/evaluation-rubric.md).
7. Evaluate every pair using the complete supplied course context.
8. Return one JSON object per pair following the output schema below.
9. Reject pairs labeled `not_related`; expand accepted pair decisions back to job-keyword rows.
10. Export the final spreadsheet with both JD evidence and course evidence.

## Required reasoning

- Resolve word meaning in context. Example: `survey course` means an overview and is not evidence of survey research.
- Do not count a skill mentioned only in prerequisites as course training.
- Do not infer training from a broad course area alone.
- Check whether students actually learn or practice the skill through outcomes, topics, projects, labs, assignments, or assessments.
- Prefer explicit teaching evidence over generic relevance.
- Preserve an exact, short excerpt from the course source as evidence.
- If evidence is ambiguous or merely transferable, choose `not_related`.

## Labels

- `direct_training`: The course explicitly teaches or practices the target skill.
- `foundational_support`: The course teaches identifiable prerequisite knowledge that materially supports later learning of the skill. State the causal bridge.
- `not_related`: The connection is lexical, generic, prerequisite-only, domain-adjacent, or unsupported.

## Output schema

```json
{
  "skill": "用户研究",
  "course_code": "ECE4300",
  "label": "direct_training",
  "confidence": 0.94,
  "evidence_field": "description_chinese",
  "evidence_quote": "包括用户界面设计、用户体验、可用性评估……",
  "reason": "课程明确教授用户体验与可用性评估，属于用户研究常用方法。",
  "foundation_bridge": "",
  "false_positive_flags": []
}
```

Use confidence from `0` to `1`. Keep `evidence_quote` under 300 characters and copy it exactly from the supplied course record. For `foundational_support`, populate `foundation_bridge`. For rejected pairs, record applicable flags such as `ambiguous_word`, `prerequisite_only`, `generic_skill`, `domain_adjacent`, or `insufficient_evidence`.

## Quality checks

- Confirm every accepted pair has a non-empty exact evidence quote.
- Confirm every foundational pair explains the foundation bridge.
- Review all pairs with confidence below `0.75`.
- Sample at least 20 accepted and 20 rejected pairs before final export.
- Report candidate, accepted, rejected, and low-confidence counts.
