# SDS Major Graduation Rules

This file records the SDS major rules currently migrated into the student dashboard rule engine.
It is intended as the human-checkable source note before the rules are moved into a standalone data file.

## Common Engine Semantics

- `mandatoryGroups`: each item is a graduation requirement. If the item contains multiple course codes, any one course satisfies the group.
- `electiveCategories`: course pools shown in the Major Progress elective section. Completed courses are marked in the UI.
- `electiveUnits`: required elective units under the major.
- `minUnitsByCategory`: a category must contribute at least the stated units.
- `maxLevelCourses`: at most the stated number of elective courses may be taken at a given course level.
- `breadth`: at least N categories must contain completed courses.
- `depth`: at least one listed category/stream must contain N completed courses.
- `streamDeclaration.optional`: stream information is displayed but does not block graduation unless the student explicitly declares a stream in a future UI.
- `exactMajorPool`: only listed mandatory/elective courses count as major units; broad prefix fallback is disabled.

## Statistics

### Admitted In 2023

Source: `study_scheme_-_stat_2022-23_and_2023-24_circularab2026_c015_3.pdf`

Total major units: 70

Mandatory:

- BIO1008
- CSC1001
- CSC1002
- DDA2001
- MAT1001 or MAT1011
- MAT1002 or MAT1012
- MAT2041 or MAT2041A
- PHY1001
- STA2001H
- MAT2050
- MAT3007 or MAT3007H
- STA2002H
- STA3001
- STA3005
- STA3020

Elective requirement:

- 27 elective units.
- Breadth: complete courses from at least 3 of Area 1, Area 2, Area 3, Area 4, and Complementary Electives.
- Depth: complete at least 3 courses from any one of Area 1, Area 2, Area 3, or Area 4.

Areas:

- Area 1 Mathematical Foundation: DDA3005, MAT3006, MAT3040, STA4001 or STA4001H, STA4100
- Area 2 Statistical Methodology: DDA4080, STA3006, STA3007, STA3030, STA3042 or STA4042, STA4002, STA4003, STA4030, STA4041, STA4102
- Area 3 Computing & Machine Learning: CSC3002, CSC3100, DDA3020 or DDA3020H, DDA4010, STA4012, STA4606
- Area 4 Financial Statistics & Risk Management: DDA3600, ECO3211, FIN3380, FMA4200, FMA4800, MAT2002, RMS4001, RMS4050, RMS4060, STA4020
- Complementary Electives: CSC3001, CSC3170, CSC4120 or CSC4120H, DDA4002, DDA4210, DDA4220, DDA4230, DDA4240, DDA4250, DDA4260, ECE2050, ECO3121, FIN2010, FIN2020, FIN3080, FIN3210, FIN4110, FIN4120, MAT3220, MAT3280, STA4005, STA4010, STA4011

### Admitted In 2024

Source: `study_scheme_-_stat_2024-25_circularab2026_c015_2.pdf`

Same structure as 2023, with the 2024 document's explicit alternatives:

- MAT3007 or MAT3007H
- DDA3020 or DDA3020H
- CSC4120 or CSC4120H
- STA3042 may replace older STA4042 for Statistical Learning according to term.

### Admitted In 2025 And Thereafter

Source: `study_scheme_-_stat_2025-26_and_thereafter_circularab2026_c015_2.pdf`

Total major units: 70

Mandatory:

- BIO1008
- CSC1001
- CSC1002
- DDA2001
- MAT1001 or MAT1011
- MAT1002 or MAT1012
- MAT2041 or MAT2041A
- PHY1001
- STA2001H
- MAT2050
- MAT3007 or MAT3007H
- STA2002H
- STA3005
- STA3020
- STA3042

Elective requirement:

- 27 elective units.
- Depth: complete at least 3 courses from any one stream.
- Stream declaration is optional. If a future UI lets a student declare a stream, a declared stream should require at least 4 courses in that stream.
- Complementary Electives count toward elective units but not stream depth.

Streams:

- Mathematical Statistics: DDA3005, DDA4002, DDA4080, MAT2002, MAT3006, MAT3040, MAT3220, MAT3280, STA4001 or STA4001H, STA4100
- Statistical Methodology: DDA4010, DDA4080, STA3001, STA3006, STA3007, STA3030, STA4002, STA4003, STA4005, STA4030, STA4041, STA4102
- Biostatistics & Bioinformatics: BIM2005, BIM3001, BIM3007, BIM3009, DDA4010, DDA4080, STA4005, STA4012, STA4030, STA4041, STA4102, STA4606
- Financial Statistics: DDA3600, DDA4010, DDA4080, ECO3211, FIN3380, FMA4200, FMA4800, MAT2002, RMS4001, RMS4050, RMS4060, STA4003, STA4020, STA4041
- Computing & Machine Learning: CSC3002, CSC3100, CSC4120 or CSC4120H, DDA3020 or DDA3020H, DDA4010, DDA4080, DDA4220
- Complementary Electives: CSC3001, CSC3170, DDA4210, DDA4230, DDA4240, DDA4250, DDA4260, ECE2050, ECO3121, FIN2010, FIN2020, FIN3080, FIN3210, FIN4110, FIN4120, STA4010, STA4011

## Computer Science And Engineering

### Admitted In 2023

Source: `Study Scheme - CSE_2023-24_Circular (AB2024_C036).pdf`

Total major units: 70

Mandatory:

- BIO1008
- CSC1003 or CSC1001
- CSC1004 or CSC1002
- DDA2001
- MAT1001
- MAT1002
- MAT2041
- PHY1001
- STA2001 or STA2001H
- CSC3001
- CSC3002
- CSC3050
- CSC3100
- CSC3150
- CSC3170
- CSC4120
- DDA3020
- ECE2050

Elective requirement:

- 18 elective units.
- Group A must contribute at least 12 units.
- Remaining elective units may be selected from Group A or Group B.

Group A:

CSC2003, CSC3120, CSC3160, CSC3180, CSC3185, CSC4001, CSC4005, CSC4008, CSC4010, CSC4012, CSC4100, CSC4130, CSC4140, CSC4150, CSC4160, CSC4180, CSC4190, CSC4303, DDA4080, DDA4210, DDA4220, DDA4230, ECE3080, ECE4016

Group B:

BIM3001, DDA3003, DDA3005, DDA4002, DDA4240, DDA4260, DDA4310, ECE3060, ECE3200, ECE4011, ECE4310, ECE4513, ECO3011, ECO3121, ECO3160, EIE3280, EIE4005, EIE4512, ERG4001, FIN3380, MAT3007, MAT3220, PHY1002, STA3005, STA4001

### Admitted In 2024 And Thereafter

Source: `Study Scheme - CSE_2024-25 and thereafter_3rd(2025)_20250714.pdf`

Total major units: 70

Mandatory:

- BIO1008
- CSC1001 or CSC1003
- CSC1002 or CSC1004
- DDA2001
- MAT1001 or MAT1011
- MAT1002 or MAT1012
- MAT2041 or MAT2041A
- PHY1001
- STA2001 or STA2001H
- CSC3001
- CSC3060
- CSC3200
- CSC3150
- CSC4120 or CSC4120H
- DDA3020 or DDA3020H

Elective requirement:

- 25 elective units.
- Group A must contribute at least 16 units.
- Remaining elective units may be selected from Group A or Group B.

Group A:

CSC2003, CSC3002, CSC3102, CSC3120, CSC3160, CSC3170, CSC3180, CSC3185, CSC4001, CSC4005, CSC4240, CSC4303, DDA4080, DDA4210, DDA4220, DDA4230, ECE3080, ECE4016

Group B:

BIM3001, DDA3003, DDA3005, DDA4002, DDA4240, DDA4260, DDA4310, ECE3060, ECE3200, ECE4011, ECE4310, ECE4513, ECO3011, ECO3121, ECO3160, EIE3280, EIE4005, EIE4512, ERG4001, FIN3380, MAT3007 or MAT3007H, MAT3220, PHY1002, STA3005, STA4001 or STA4001H

## Data Science And Big Data Technology

### Admitted In 2023

Source: `Study Scheme - DS_2023-24_3rd(2025)_20250714.pdf`

Total major units: 70

Mandatory:

- BIO1008
- CSC1001
- CSC1002
- DDA2001
- MAT1001
- MAT1002
- MAT2041
- PHY1001
- STA2001 or STA2001H
- CSC3100
- DDA3020
- DDA4002
- MAT3007
- STA2002 or STA2002H
- STA4001

Elective requirement:

- 27 elective units selected from streams.
- At most two 2000-level elective courses.

### Admitted In 2024 And Thereafter

Source: `Study Scheme - DS_2024-25 and thereafter_3rd(2025)_20250714.pdf`

Total major units: 70

Mandatory:

- BIO1008
- CSC1001
- CSC1002
- DDA2001
- MAT1001 or MAT1011
- MAT1002 or MAT1012
- MAT2041 or MAT2041A
- PHY1001
- STA2001 or STA2001H
- CSC3100
- DDA3020 or DDA3020H
- DDA4002
- MAT3007 or MAT3007H
- STA2002 or STA2002H
- STA4001 or STA4001H

Elective requirement:

- 27 elective units selected from streams.
- At most two 2000-level elective courses.
- Stream specialization is optional in the current UI. If declared later, the chosen stream should require at least 4 courses.

Streams:

- Methodology and Theory: CSC3001, DDA3003, DDA3005, DDA4010, DDA4080, DDA4210, DDA4230, DDA4240, DDA4250, DDA4320, MAT2002, MAT2050, MAT3220, MAT3280, STA3020, STA3005
- Finance and Economics: DDA4080, ECO3121, ECO3160, ECO3211, ECO4121, FIN3080, FMA4200, FMA4800, STA4002, STA4003, STA4020
- Operations Management: DDA4080, DDA4260, DMS2030, DMS3210, DMS4010, DMS4031, ECO3160, MGT2020, MKT4220
- Life Science: BIM2005, BIM2006, BIM3001, BIM3007, BIM3009, BIO3204, BIO3213, DDA4080, STA4012
- Computing: CSC2003, CSC3001, CSC3002, CSC3050, CSC3150, CSC3160, CSC3170, CSC4001, CSC4005, CSC4010, CSC4107, CSC4120 or CSC4120H, CSC4140, CSC4150, CSC4160, CSC4170, DDA4080, DDA4220, DDA4310, ERG3010

## Migration Notes

- Statistics migrated cleanly after adding breadth/depth and optional stream handling.
- CSE required `minUnitsByCategory` because elective group A has a minimum unit threshold.
- DSBDT required `maxLevelCourses` because at most two 2000-level electives can count.
- Optional streams are displayed but not treated as blockers until the UI lets the student declare a stream.
