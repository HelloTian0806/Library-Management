from pathlib import Path
import xml.etree.ElementTree as E
from PIL import Image, ImageDraw, ImageFont

OUT=Path(__file__).parent
DOC=E.Element('mxfile',host='app.diagrams.net',type='device')
BASE='html=0;whiteSpace=wrap;fontFamily=Microsoft JhengHei;fontSize=16;fontColor=#24374D;'
class Page:
    def __init__(self,name,w,h,note):
        self.w,self.h=w,h;self.serial=1;self.boxes={};self.shapes=[];self.lines=[]
        d=E.SubElement(DOC,'diagram',id='p'+str(len(DOC)+1),name=name)
        model=E.SubElement(d,'mxGraphModel',grid='1',gridSize='10',page='1',pageScale='1',pageWidth=str(w),pageHeight=str(h))
        self.root=E.SubElement(model,'root');E.SubElement(self.root,'mxCell',id='0');E.SubElement(self.root,'mxCell',id='1',parent='0')
        self.text(name,40,20,w-80,45,24)
        self.text(note,40,72,w-80,90,16)
    def vertex(self,text,x,y,w,h,style='',parent='1'):
        self.serial+=1; ident=str(self.serial)
        c=E.SubElement(self.root,'mxCell',id=ident,value=text,style=BASE+style,vertex='1',parent=parent)
        E.SubElement(c,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),attrib={'as':'geometry'})
        return ident
    def text(self,t,x,y,w,h,size=16):
        self.vertex(t,x,y,w,h,f'text;align=left;verticalAlign=top;fontSize={size};')
        self.shapes.append((t,x,y,w,h,None,size))
    def cls(self,key,title,stereo,attrs,ops,x,y,w=390):
        if not attrs and not ops:
            ident=self.vertex(f'«{stereo}»\n{title}',x,y,w,72,'rounded=0;fillColor=#E4F0E6;strokeColor=#63798F;align=left;verticalAlign=top;spacing=8;')
            self.boxes[key]=(ident,x,y,w,72)
            self.shapes.append((f'«{stereo}»\n{title}',x,y,w,72,'#E4F0E6',16))
            return key
        ah=max(44,24*len(attrs)+16);oh=max(44,24*len(ops)+16);hh=64;h=hh+ah+oh
        group=self.vertex('',x,y,w,h,'group;');self.boxes[key]=(group,x,y,w,h)
        color={'entity':'#E4F0E6','control':'#FFF1D1','boundary':'#E5EFFB','enumeration':'#EEE6F7'}[stereo]
        for text,dy,ch,bg in [(f'«{stereo}»\n{title}',0,hh,color),('\n'.join(attrs) or '—',hh,ah,'#FFFFFF'),('\n'.join(ops) or '—',hh+ah,oh,'#FFFFFF')]:
            self.vertex(text,0,dy,w,ch,f'rounded=0;fillColor={bg};strokeColor=#63798F;align=left;verticalAlign=top;spacing=8;',group)
            self.shapes.append((text,x,y+dy,w,ch,bg,16))
        return key
    def link(self,a,b,label='',mult=None,sy=.5,ty=.5):
        ga,xa,ya,wa,ha=self.boxes[a];gb,xb,yb,wb,hb=self.boxes[b]
        right=xb>xa
        ax=xa+wa if right else xa;bx=xb if right else xb+wb
        ay=ya+ha*sy;by=yb+hb*ty
        self.serial+=1
        style=BASE+'edgeStyle=orthogonalEdgeStyle;rounded=0;labelBackgroundColor=#FFFFFF;strokeColor=#60788D;'
        style+=('dashed=0;endArrow=none;' if mult else 'dashed=1;endArrow=open;endFill=0;')
        style+=f'exitX={1 if right else 0};exitY={sy};entryX={0 if right else 1};entryY={ty};'
        c=E.SubElement(self.root,'mxCell',id=str(self.serial),value=label,edge='1',parent='1',source=ga,target=gb,style=style)
        E.SubElement(c,'mxGeometry',relative='1',attrib={'as':'geometry'})
        self.lines.append((ax,ay,bx,by,label,not bool(mult)))
        if mult:
            self.text(mult[0],ax+6,ay-28,70,25);self.text(mult[1],bx-55,by-28,50,25)
    def preview(self,path):
        im=Image.new('RGB',(self.w,self.h),'white');draw=ImageDraw.Draw(im)
        fontpath='C:/Windows/Fonts/msjh.ttc'
        for ax,ay,bx,by,label,dash in self.lines:
            mid=(ax+bx)/2
            draw.line([(ax,ay),(mid,ay),(mid,by),(bx,by)],fill='#60788D',width=2)
            if dash:draw.line([(bx-10,by-5),(bx,by),(bx-10,by+5)],fill='#60788D',width=2)
            draw.text((mid-30,(ay+by)/2-23),label,font=ImageFont.truetype(fontpath,15),fill='#24374D')
        for text,x,y,w,h,bg,size in self.shapes:
            if bg:draw.rectangle((x,y,x+w,y+h),fill=bg,outline='#63798F',width=2)
            draw.multiline_text((x+8,y+6),text,font=ImageFont.truetype(fontpath,size),fill='#24374D',spacing=5)
        im.save(path)

member=['- id: int','- name: String','- email: String','- password: String','- phone: String','- createdAt: LocalDate','- account: String {設計補充}','- role: Role {設計補充}','- status: MemberStatus {設計補充}']
book=['- id: int','- isbn: String {unique}','- title: String','- author: String','- publisher: String','- category: String','- totalQuantity: int','- availableQuantity: int']
borrow=['- id: int','- memberId: int','- bookId: int','- borrowDate: LocalDate','- dueDate: LocalDate','- returnDate: LocalDate [0..1]','- status: BorrowStatus']
p=Page('01 核心實體類別圖',1650,1100,'依使用案例與現有 Entity 設計；{設計補充} 表示目前程式尚未定義。\n實線＝關聯，端點數字＝多重性；虛線箭頭＝使用／型別依賴。+ 公開；- 私有。')
p.cls('member','會員 Member','entity',member,['+ getId(): int','+ getName(): String','（其餘存取方法省略）'],40,200,420)
p.cls('borrow','借閱紀錄 Borrow','entity',borrow,['+ getStatus(): BorrowStatus','+ getReturnDate(): LocalDate','（其餘存取方法省略）'],615,200,420)
p.cls('book','書籍 Book','entity',book,['+ getAvailableQuantity(): int','+ getTotalQuantity(): int','（其餘存取方法省略）'],1190,200,420)
p.link('member','borrow','借閱',('1','0..*'),.42,.42)
p.link('borrow','book','對應',('0..*','1'),.64,.64)
p.cls('role','Role {設計補充}','enumeration',['MEMBER','ADMIN'],[],40,760,300)
p.cls('state','MemberStatus {設計補充}','enumeration',['ACTIVE','SUSPENDED'],[],390,760,310)
p.cls('borrowstate','BorrowStatus','enumeration',['BORROWED','RETURNED','OVERDUE'],[],850,760,340)
p.link('member','role','role',sy=.9,ty=.15)
p.link('member','state','status',sy=.95,ty=.15)
p.link('borrow','borrowstate','status',sy=.9,ty=.15)
p.text('關聯規則：每筆 Borrow 對應 1 位會員與 1 本書籍；會員／書籍可有 0..* 筆歷史借閱。\n數量約束：0 ≤ availableQuantity ≤ totalQuantity。帳號採 account 建模；是否改用 email 登入仍待實作決定。',40,980,1550,100)
pages=[p]

def workflow_page(name,rows,footer=''):
    h=220+len(rows)*380+130
    p=Page(name,1770,h,'BCE 分析設計：左＝Boundary 介面；中＝Control 控制；右＝Entity 實體。虛線箭頭＝依賴。\n操作名稱是依使用案例提出的分析責任，並非宣稱目前 Java 程式已具備同名方法。')
    for i,(ui,uiops,control,ops,entities) in enumerate(rows):
        y=190+i*380
        p.cls(f'b{i}',ui,'boundary',[],uiops,40,y,470)
        p.cls(f'c{i}',control,'control',[],ops,650,y,520)
        p.link(f'b{i}',f'c{i}','呼叫')
        for j,(ename,attrs) in enumerate(entities):
            key=f'e{i}_{j}'
            p.cls(key,ename,'entity',attrs,[],1340,y+j*115,390)
            p.link(f'c{i}',key,'使用',sy=(j+1)/(len(entities)+1),ty=.35)
    p.text(footer or '同名 Entity 為核心實體類別的重複視圖；屬性及存取方法詳見第 01 頁。\n控制類別對應前述 AC 循序圖的責任；實作時可合併至適當的 Service。',40,h-110,1680,95)
    pages.append(p)

workflow_page('02 會員與登入類別圖',[
('註冊介面',['+ 顯示表單()','+ 送出註冊資料()','+ 顯示結果()'],'註冊控制',['+ 驗證資料(資料): boolean','+ 檢查帳號(帳號): boolean','+ 註冊(資料): Member'],[('會員 Member',[])]),
('登入介面',['+ 送出帳號密碼()','+ 依角色導向頁面()'],'登入控制',['+ 登入(帳號, 密碼): Member','+ 檢查停權(會員): boolean','+ 建立登入狀態(會員)'],[('會員 Member',[])]),
('個人資料介面',['+ 顯示個人資料()','+ 送出允許修改的欄位()'],'會員資料控制',['+ 查詢個人資料(會員ID): Member','+ 驗證可修改欄位(資料): boolean','+ 更新個人資料(會員ID, 資料): boolean'],[('會員 Member',[])]),
('會員管理介面',['+ 顯示會員列表()','+ 搜尋或選擇會員()','+ 送出會員資料與狀態()'],'會員管理控制',['+ 搜尋會員(條件): List<Member>','+ 查看會員(會員ID): Member','+ 更新會員資料(會員ID, 資料): boolean','+ 更新會員狀態(會員ID, 狀態): boolean'],[('會員 Member',[])])])
workflow_page('03 書籍管理類別圖',[
('書籍搜尋／資訊介面',['+ 送出搜尋條件()','+ 顯示搜尋結果()','+ 顯示書籍資訊與庫存()'],'書籍查詢控制',['+ 搜尋書籍(關鍵字): List<Book>','+ 查詢全部書籍(): List<Book>','+ 查看書籍(書籍ID): Book'],[('書籍 Book',[])]),
('書籍管理介面',['+ 顯示書籍列表()','+ 顯示新增／修改表單()','+ 送出書籍資料()'],'書籍管理控制',['+ 驗證資料(資料): boolean','+ 檢查ISBN(ISBN, 排除書籍ID): boolean','+ 新增書籍(資料): boolean','+ 更新書籍(書籍ID, 資料): boolean'],[('書籍 Book',[])])],
'書籍 ISBN 不可與其他書籍重複；修改時排除自己的書籍 ID。空白搜尋採「顯示全部書籍」。\n原使用案例未定義刪除書籍，本分析圖不加入刪除操作。')
workflow_page('04 借閱與歸還類別圖',[
('書籍資訊／借閱確認介面',['+ 顯示借閱確認資訊()','+ 確認借閱()','+ 取消借閱()'],'借閱控制',['+ 檢查借閱資格(會員ID): boolean','+ 檢查庫存(書籍ID): boolean','+ 借閱(會員ID, 書籍ID): Borrow','+ 提交交易()','+ 回復交易()'],[('會員 Member',[]),('書籍 Book',[]),('借閱紀錄 Borrow',[])]),
('歸還書籍介面',['+ 掃描書籍條碼()','+ 顯示有效借閱資訊()','+ 確認／取消歸還()'],'歸還控制',['+ 查詢書籍(條碼): Book','+ 查詢有效借閱(書籍ID): List<Borrow>','+ 歸還(借閱ID): boolean','+ 提交交易()','+ 回復交易()'],[('書籍 Book',[]),('借閱紀錄 Borrow',[])])],
'交易約束：Borrow 建立／歸還與 Book 可借閱數量更新必須同時成功；失敗皆回復，取消不修改資料。\n待明確化：原文件以條碼查書，但現有 Book 未定義條碼；多冊同書也可能對應多筆有效借閱，需辨識實際歸還紀錄。')
workflow_page('05 借閱查詢類別圖',[
('個人借閱介面',['+ 開啟個人借閱()','+ 顯示借閱日期、歸還日與狀態()'],'個人借閱控制',['+ 確認登入(): boolean','+ 取得登入會員ID(): int','+ 查詢個人借閱(會員ID): List<Borrow>'],[('會員 Member',[]),('借閱紀錄 Borrow',[])]),
('借閱管理介面',['+ 輸入搜尋條件()','+ 顯示借閱列表()','+ 顯示借閱明細()'],'借閱管理控制',['+ 查詢借閱(會員, 書籍, 狀態): List<Borrow>','+ 查看借閱明細(借閱ID): Borrow'],[('借閱紀錄 Borrow',[])])],
'會員僅查詢登入身分所屬紀錄；管理者可依會員、書籍與借閱狀態查詢。\n控制層處理查無資料等例外；借阅資訊中的書籍識別由 Borrow.bookId 對應 Book。')

path=OUT/'圖書管理系統_類別圖.drawio'
E.indent(DOC);E.ElementTree(DOC).write(path,encoding='utf-8',xml_declaration=True)
for page in DOC:
    cells=page.findall('./mxGraphModel/root/mxCell');ids=[c.get('id') for c in cells]
    assert len(ids)==len(set(ids))
    for c in cells:
        if c.get('edge')=='1':assert c.get('source') in ids and c.get('target') in ids
for i,p in enumerate(pages,1):
    for _,x,y,w,h in p.boxes.values():assert x>=0 and y>=0 and x+w<=p.w and y+h<=p.h
    p.preview(OUT/f'class-layout-{i}.png')
print('Created and validated 5 editable class diagram pages and layout previews.')
