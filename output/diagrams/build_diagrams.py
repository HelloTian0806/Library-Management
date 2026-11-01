from pathlib import Path
import xml.etree.ElementTree as E

OUT=Path(__file__).parent
doc=E.Element('mxfile',host='app.diagrams.net',type='device')
counts=[]
def diagram(title,pre,steps,note=''):
    page=E.SubElement(doc,'diagram',id=f'p{len(counts)+1}',name=title)
    height=220+len(steps)*130
    model=E.SubElement(page,'mxGraphModel',dx='1200',dy='900',grid='1',gridSize='10',guides='1',tooltips='1',connect='1',arrows='1',fold='1',page='1',pageScale='1',pageWidth='1160',pageHeight=str(height+100),math='0',shadow='0')
    root=E.SubElement(model,'root'); E.SubElement(root,'mxCell',id='0'); E.SubElement(root,'mxCell',id='1',parent='0')
    counter=0
    def node(text,x,y,w=340,h=64,kind='action'):
        nonlocal counter
        counter+=1; ident=str(counter+1)
        styles={'action':'rounded=1;arcSize=20;fillColor=#EAF2FB;strokeColor=#527CA6;', 'decision':'rhombus;fillColor=#FFF2CC;strokeColor=#B49A43;', 'error':'rounded=1;arcSize=20;fillColor=#FCE8E6;strokeColor=#BD716B;', 'start':'ellipse;fillColor=#20354B;strokeColor=#20354B;', 'end':'shape=doubleEllipse;fillColor=#20354B;strokeColor=#20354B;', 'text':'text;strokeColor=none;fillColor=none;align=left;'}
        c=E.SubElement(root,'mxCell',id=ident,value=text,style=styles[kind]+'whiteSpace=wrap;html=0;fontFamily=Microsoft JhengHei;fontSize=16;fontColor=#20354B;spacing=8;',vertex='1',parent='1')
        E.SubElement(c,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),attrib={'as':'geometry'})
        return ident
    def edge(a,b,label='',side=False):
        nonlocal counter
        counter+=1
        style='edgeStyle=orthogonalEdgeStyle;rounded=0;html=0;endArrow=block;endFill=1;strokeColor=#52677D;fontFamily=Microsoft JhengHei;fontSize=14;labelBackgroundColor=#FFFFFF;'
        style+='exitX=1;exitY=0.5;entryX=0;entryY=0.5;' if side else 'exitX=0.5;exitY=1;entryX=0.5;entryY=0;'
        c=E.SubElement(root,'mxCell',id=str(counter+1),value=label,style=style,edge='1',parent='1',source=a,target=b)
        E.SubElement(c,'mxGeometry',relative='1',attrib={'as':'geometry'})
    node(title,40,20,1080,40,'text')
    node('前置條件：'+pre+'\n'+('註：'+note if note else '圖例：● 開始　◉ 活動結束　菱形＝判斷；方括號＝分支條件。'),40,65,1080,65,'text')
    prev=node('',270,145,24,24,'start'); label=''
    for i,step in enumerate(steps):
        y=210+i*130
        if isinstance(step,str):
            cur=node(step,110,y)
            edge(prev,cur,label); label='';prev=cur
        else:
            question,ok,bad,result=step
            cur=node(question,150,y-8,260,88,'decision');edge(prev,cur,label)
            exc=node(result,620,y,350,70,'error');edge(cur,exc,'['+bad+']',True)
            end=node('',1030,y+22,26,26,'end');edge(exc,end,side=True)
            prev=cur;label='['+ok+']'
    end=node('',268,210+len(steps)*130,28,28,'end');edge(prev,end,label)
    counts.append((title,len(steps)))

diagram('01 會員註冊','使用者尚未註冊會員',[
'使用者：進入註冊頁面','使用者：輸入帳號、密碼及基本會員資料\n送出註冊資料',('系統：欄位格式正確？','是','否','系統：顯示錯誤欄位，要求重新輸入\n本次送出結束，修正後重新送出'),('系統：帳號已存在？','否','是','系統：提示帳號已被使用'),'系統：建立會員資料',('系統：建立成功？','是','否','系統：不建立會員資料\n顯示註冊失敗'),'系統：顯示註冊成功\n導向登入頁面'])
diagram('02 會員登入','已具有有效帳號',[
'使用者／管理者：進入登入頁面\n輸入帳號及密碼',('系統：帳號存在？','是','否','系統：顯示登入失敗'),('系統：密碼正確？','是','否','系統：顯示登入失敗'),('系統：帳號遭停權？','否','是','系統：拒絕登入並顯示相關訊息'),'系統：建立登入狀態\n確認使用者角色',('系統：使用者為管理者？','否：一般會員','是：管理者','系統：導向管理頁面'),'系統：導向會員頁面'])
diagram('03 管理會員資料－會員','會員已登入系統',[
'會員：進入個人資料頁面','系統：顯示會員資料','會員：修改允許修改的資料\n送出修改',('系統：資料格式正確？','是','否','系統：顯示錯誤欄位'),'系統：更新會員資料',('系統：更新成功？','是','否','系統：保留原資料\n顯示操作失敗'),'系統：顯示修改成功'])
diagram('04 管理會員資料－管理者','管理者已登入系統',[
'管理者：進入會員管理頁面','系統：顯示會員列表','管理者：搜尋或選擇會員',('系統：會員存在？','是','否','系統：顯示會員不存在'),'系統：顯示該會員資料','管理者：修改會員資料或會員狀態',('系統：資料格式正確？','是','否','系統：顯示錯誤欄位'),'系統：儲存修改',('系統：儲存成功？','是','否','系統：保留原資料\n顯示操作失敗'),'系統：顯示操作成功'])
diagram('05 搜尋書籍','無',[
'使用者：進入書籍搜尋頁面','使用者：輸入書名或關鍵字\n送出搜尋',('系統：有輸入關鍵字？','是','否','系統：顯示全部書籍'),'系統：依搜尋條件查詢書籍',('系統：有符合條件的書籍？','是','否','系統：顯示「查無符合條件的書籍」'),'系統：顯示搜尋結果'], '原文件允許空白搜尋顯示全部書籍或提示輸入條件；本圖採顯示全部書籍。')
diagram('06 查看書籍資訊','書籍存在於系統中',[
'使用者：從書籍列表選擇書籍','系統：依書籍 ID 查詢書籍資料',('系統：書籍存在？','是','否','系統：顯示「查無此書籍」'),'系統：顯示書籍詳細資訊','系統：顯示目前可借閱數量及借閱狀態'])
diagram('07 借閱書籍','會員已登入、帳號有效，書籍存在且有可借閱數量',[
'會員：進入書籍資訊頁面\n點選「借閱」',('系統：會員已登入？','是','否','系統：導向登入頁面'),('系統：帳號可借閱？','是','否','系統：拒絕借閱'),('系統：可借閱數量大於 0？','是','否','系統：顯示「目前無可借閱書籍」'),'系統：顯示借閱確認資訊',('會員：確認借閱？','確認','取消','系統：不修改任何資料'),'系統：於同一交易中建立借閱紀錄\n設定借閱日期及應歸還日期\n將可借閱數量減少 1',('系統：交易成功？','是','否','系統：Rollback Transaction\n借閱紀錄與書籍數量維持原狀'),'系統：提交交易，顯示借閱成功','會員：可於「個人借閱」查看該紀錄'], '前置條件仍依原文件於流程內再次檢查；借閱紀錄與數量更新屬同一交易。')
diagram('08 查看個人借閱','會員已登入',[
'會員：點選「個人借閱」',('系統：會員已登入？','是','否','系統：導向登入頁面'),'系統：取得目前登入會員 ID','系統：查詢該會員借閱紀錄',('系統：有借閱紀錄？','是','否','系統：顯示「目前沒有借閱紀錄」'),'系統：顯示借閱書籍、借閱日期、\n應歸還日期、實際歸還日期及借閱狀態'])
diagram('09 辦理歸還','管理者已登入；書籍存在有效且尚未歸還的借閱紀錄',[
'管理者：進入歸還書籍功能\n掃描書籍條碼','系統：依條碼查詢書籍',('系統：書籍存在？','是','否','系統：顯示「查無此書籍」'),'系統：查詢該書籍目前有效的借閱紀錄',('系統：有有效且未歸還紀錄？','是','否','系統：顯示\n「此書籍目前沒有借閱紀錄」'),'系統：顯示借閱資訊',('管理者：確認歸還？','確認','取消','系統：不修改任何資料'),'系統：於同一交易中更新為已歸還\n記錄實際歸還時間\n將可借閱數量增加 1',('系統：交易成功？','是','否','系統：Rollback Transaction\n借閱紀錄與書籍數量維持原狀\n顯示「歸還失敗」'),'系統：提交交易，顯示歸還成功'])
diagram('10 管理書籍','管理者已登入',[
'管理者：進入書籍管理頁面','系統：顯示書籍列表','管理者：選擇新增或修改書籍','系統：依選擇顯示對應表單','管理者：輸入或修改 ISBN、書名、\n作者、出版社、館藏數量等資料\n送出資料',('系統：資料格式正確？','是','否','系統：顯示錯誤欄位'),('系統：ISBN 檢查通過？','是','否','新增：ISBN 已存在，拒絕新增並提示重複\n修改：與其他書籍重複，拒絕修改'),'系統：依選擇新增或更新書籍資料',('系統：資料庫操作成功？','是','否','系統：保留原資料\n顯示操作失敗'),'系統：顯示操作成功'], 'ISBN 檢查：新增不得與既有書籍重複；修改不得與其他書籍重複。')
diagram('11 管理借閱','管理者已登入',[
'管理者：進入借閱管理頁面','系統：顯示借閱紀錄列表','管理者：輸入會員、書籍或借閱狀態等條件','系統：查詢符合條件的借閱紀錄',('系統：查到符合條件的紀錄？','是','否','系統：顯示\n「查無符合條件的借閱紀錄」'),'系統：顯示搜尋結果','管理者：選擇借閱紀錄',('系統：所選紀錄仍存在？','是','否','系統：顯示「借閱紀錄不存在」'),'系統：顯示借閱詳細資料'], '原文件主要流程只定義查詢與查看明細；未另行增加修改借閱紀錄的操作。')

path=OUT/'圖書管理系統_活動圖.drawio'
E.indent(doc)
E.ElementTree(doc).write(path,encoding='utf-8',xml_declaration=True)
# Validate each page: unique IDs, valid edge references and guarded decisions.
for page in E.parse(path).getroot():
    cells=page.findall('./mxGraphModel/root/mxCell'); ids=[c.get('id') for c in cells]
    assert len(ids)==len(set(ids))
    for c in cells:
        if c.get('edge')=='1': assert c.get('source') in ids and c.get('target') in ids
        if 'rhombus;' in c.get('style',''):
            links=[e for e in cells if e.get('source')==c.get('id')]
            assert len(links)==2 and all(e.get('value') for e in links)
print(f'Validated {len(counts)} pages: {path}')
