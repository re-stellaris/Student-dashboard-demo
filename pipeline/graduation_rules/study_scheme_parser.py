import re

TOKEN=re.compile(r"(?<![A-Za-z0-9])(?:(?P<p>[A-Z]{2,5})\s*)?(?P<n>[1-6]\d{3}[A-Z]?)(?![A-Za-z0-9])")

def course_rows(text):
    """Only contiguous rows beginning with course codes; carry subject within a list.
    Never scans narrative dates or inherits a prefix from an earlier paragraph.
    """
    rows=[]; current=[]
    for line in text.splitlines():
        s=line.strip()
        s=re.sub(r'^\([a-z]\)\s*','',s)
        if re.search(r':\s*(?=[A-Z]{2,5}\s*[1-6]\d{3})',s):
            if current:rows.append(' '.join(current));current=[]
            s=s.split(':',1)[1].strip()
        if re.match(r'^\(?(?:[A-Z]{2,5}\s*)?[1-6]\d{3}[A-Z]?(?:\W|$)',s) or re.match(r'^(?:CBS|UBC)[12](?:st|nd)',s) or (current and re.match(r'^or\s+(?:[A-Z]{2,5})?[1-6]\d{3}',s)):
            s=re.sub(r'\s+\d{1,2}\s*$','',s)
            current.append(s)
        else:
            if current:rows.append(' '.join(current));current=[]
    if current:rows.append(' '.join(current))
    return rows

def expand_row(row):
    row=re.sub(r'\b([A-Z]{2,5}),\s*(?=[1-6]\d{3})',r'\1',row)
    prefix=None; codes=[]; pieces=[]; last=0;stack=[];cursor=0;alternative_base=None
    for m in TOKEN.finditer(row):
        gap=row[cursor:m.start()]
        if alternative_base and re.search(r'[,;]',gap):prefix=alternative_base;alternative_base=None
        for ch in gap:
            if ch=='(':stack.append(prefix)
            elif ch==')' and stack:prefix=stack.pop()
        if not stack and re.search(r'/|\bor\b',gap) and alternative_base is None:alternative_base=prefix
        if m['p']:prefix=m['p']
        if prefix:
            code=prefix+m['n'];codes.append(code)
            pieces.extend([row[last:m.start()],code]);last=m.end()
        cursor=m.end()
    pieces.append(row[last:])
    # Exchange slots don't fit normal catalogue codes.
    codes.extend(re.findall(r'\b(?:CBS|UBC)[12](?:st|nd)\b',row))
    return list(dict.fromkeys(codes)),''.join(pieces)
