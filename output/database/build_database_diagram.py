from pathlib import Path
import re
import xml.etree.ElementTree as ET

BASE_DIR=Path(__file__).parent
helper=BASE_DIR.parent/'diagrams'/'build_class_diagram.py'
exec(helper.read_text(encoding='utf-8').split("member=[")[0])
OUT=BASE_DIR
sql=(OUT/'schema.sql').read_text(encoding='utf-8')
p=Page('01 圖書管理系統資料庫 ER 圖',2040,1110,
       '目標設計：MySQL 8.0.16+／InnoDB／utf8mb4；依現有三表模型擴充。\nPK＝主鍵，FK＝外鍵，UK＝唯一；NN＝不可空值。★ 為新增設計欄位，需同步調整程式。')
added={'members':{'account','role','status'},'books':set(),'borrows':{'returned_at'}}
labels={'members':'會員／管理者','books':'書目／合計館藏','borrows':'借閱歷史'}
positions={'members':40,'borrows':750,'books':1460}
constraints={
 'members':['role：MEMBER / ADMIN','status：ACTIVE / SUSPENDED','account 唯一；password 僅存密碼雜湊','索引：name'],
 'books':['ISBN 唯一；長度 10 或 13','0 <= 可借數量 <= 總數量','索引：title'],
 'borrows':['status：BORROWED / RETURNED / OVERDUE','due_date 不早於 borrow_date','已歸還時，日期與時間須同時填寫','外鍵：ON DELETE / UPDATE RESTRICT','索引：(member_id, borrow_date)','索引：(book_id, status)；(status, due_date)']}
allcols={}
for table,body in re.findall(r'CREATE TABLE (\w+) \((.*?)\) ENGINE=',sql,re.S):
    cols=[]
    for line in body.splitlines():
        match=re.match(r'\s+(\w+) (INT|VARCHAR\(\d+\)|DATE(?:TIME)?)(.*)',line)
        if match:
            name,typ,rest=match.groups()
            mark='PK' if name=='id' else 'FK' if name in ('member_id','book_id') else 'UK' if name in ('account','isbn') else ''
            nullable='NN' if 'NOT NULL' in rest else 'NULL'
            cols.append((name,typ,mark,nullable))
    allcols[table]=cols
    x=positions[table];y=205;w=540;head=68;rh=39;ch=35+len(constraints[table])*27
    h=head+rh*len(cols)+ch
    group=p.vertex('',x,y,w,h,'group;');p.boxes[table]=(group,x,y,w,h)
    title=table+'｜'+labels[table]
    p.vertex(title,0,0,w,head,'fillColor=#DCEAF7;strokeColor=#60788D;fontSize=22;align=left;spacing=10;',group)
    p.shapes.append((title,x,y,w,head,'#DCEAF7',22))
    for i,(name,typ,mark,nullable) in enumerate(cols):
        text=f'{mark:2}  {name}'+(' ★' if name in added[table] else '')+f' : {typ}  {nullable}'
        dy=head+i*rh
        color='#F7FAFD' if i%2==0 else '#FFFFFF'
        p.vertex(text,0,dy,w,rh,f'fillColor={color};strokeColor=#A9BAC9;align=left;spacing=8;',group)
        p.shapes.append((text,x,y+dy,w,rh,color,16))
    text='限制與索引\n'+'\n'.join(constraints[table]);dy=head+rh*len(cols)
    p.vertex(text,0,dy,w,ch,'fillColor=#F2F5F8;strokeColor=#60788D;align=left;verticalAlign=top;spacing=8;',group)
    p.shapes.append((text,x,y+dy,w,ch,'#F2F5F8',16))
p.link('members','borrows','會員借閱',('1','0..*'),.28,.28)
p.link('borrows','books','對應書目',('0..*','1'),.5,.5)
p.text('關聯：每筆借閱必須對應 1 位會員及 1 個書目；一位會員／一個書目可有 0 到多筆借閱歷史。\n交易：借閱紀錄與庫存增減須同一交易提交或回復；應用層必須檢查每次更新影響筆數。\n條碼：本版掃描 ISBN 後選擇實際借閱紀錄；逐冊唯一條碼需另增 book_copies，未包含於本版。\n本檔為目標設計，未修改現有資料庫；詳細預設值、交易規則與程式銜接見 schema.sql 及設計說明。',40,900,1940,175)
path=OUT/'圖書管理系統_資料庫ER圖.drawio'
ET.indent(DOC);ET.ElementTree(DOC).write(path,encoding='utf-8',xml_declaration=True)
assert set(allcols)=={'members','books','borrows'}
assert len(allcols['members'])==9 and len(allcols['books'])==8 and len(allcols['borrows'])==8
for _,x,y,w,h in p.boxes.values():assert x+w<=p.w and y+h<900
parsed=ET.parse(path)
cells=parsed.findall('.//mxCell');ids={c.get('id') for c in cells}
assert len(ids)==len(cells)
for c in cells:
    if c.get('edge')=='1':assert c.get('source') in ids and c.get('target') in ids
p.preview(OUT/'資料庫ER圖_預覽.png')
print('Validated ER diagram against schema.sql: 3 tables, 25 columns, 2 relationships.')
