from pathlib import Path
import xml.etree.ElementTree as ET

OUT = Path(__file__).parent
DOC = ET.Element('mxfile', host='app.diagrams.net', type='device')
PAGES = []
def m(a,b,t): return ('msg',a,b,t)
def fail(guard,text): return ('break',guard,[m(2,1,text),m(1,0,text)])
def alt(*branches): return ('alt',branches)
def show(text): return m(1,0,text)
def act(text): return m(0,1,text)
def req(text): return m(1,2,text)
def ret(text): return m(2,1,text)
def page(name,actor,ui,pre,events,note=''):
    PAGES.append((name,actor,ui,pre,events,note))

page('01 會員註冊','使用者','註冊介面','尚未註冊會員',[
act('開啟註冊頁面'),show('顯示註冊表單'),act('填寫帳號、密碼及基本資料；送出'),req('提交註冊資料並驗證'),
fail('欄位格式錯誤','標示錯誤欄位，要求重新輸入'),fail('帳號已存在','提示帳號已被使用'),
m(2,2,'建立會員資料'),fail('建立會員失敗','註冊失敗；不建立會員資料'),ret('註冊成功'),show('顯示註冊成功並導向登入頁面')])
page('02 會員登入','使用者／管理者','登入介面','已具有有效帳號',[
act('開啟登入頁面'),show('顯示登入表單'),act('輸入帳號、密碼並送出'),req('驗證帳號及密碼'),
fail('帳號不存在或密碼錯誤','顯示登入失敗'),fail('帳號遭停權','拒絕登入並顯示停權訊息'),
m(2,2,'建立登入狀態並判定角色'),ret('登入成功及使用者角色'),alt(('一般會員',[show('導向會員頁面')]),('管理者',[show('導向管理頁面')]))])
page('03 管理會員資料－會員','會員','個人資料介面','會員已登入',[
act('開啟個人資料頁面'),req('取得目前會員資料'),ret('會員資料'),show('顯示會員資料與可編輯欄位'),
act('修改允許的欄位並送出'),req('驗證並更新會員資料'),fail('資料格式錯誤','顯示錯誤欄位'),
fail('更新失敗','操作失敗；保留原資料'),ret('更新成功'),show('顯示修改成功及更新後資料')])
page('04 管理會員資料－管理者','管理者','會員管理介面','管理者已登入',[
act('進入會員管理頁面'),req('取得會員列表'),ret('會員列表'),show('顯示會員列表'),act('搜尋或選擇會員'),req('查詢指定會員'),
fail('會員不存在','顯示會員不存在'),ret('會員詳細資料'),show('顯示會員資料與狀態'),act('修改會員資料或狀態並儲存'),
req('驗證並儲存修改'),fail('資料格式錯誤','顯示錯誤欄位'),fail('儲存失敗','操作失敗；保留原資料'),ret('儲存成功'),show('顯示操作成功')])
page('05 搜尋書籍','使用者','書籍搜尋介面','無',[
act('進入書籍搜尋頁面'),show('顯示搜尋欄位'),act('輸入書名或關鍵字；送出搜尋'),
alt(('有輸入關鍵字',[req('依關鍵字查詢書籍')]),('未輸入關鍵字',[req('查詢全部書籍')])),ret('書籍查詢結果'),
alt(('有符合條件的書籍',[show('顯示書籍搜尋結果列表')]),('無符合條件的書籍',[show('顯示「查無符合條件的書籍」')]))],
'空白搜尋採原文件允許的「顯示全部書籍」方案。')
page('06 查看書籍資訊','使用者','書籍資訊介面','書籍存在於系統中',[
act('從列表選擇書籍'),req('依書籍 ID 查詢詳細資料'),fail('書籍不存在','顯示「查無此書籍」'),
ret('書籍資訊、可借閱數量及借閱狀態'),show('顯示書籍詳細資料、可借閱數量及狀態')])
page('07 借閱書籍','會員','書籍資訊／借閱確認介面','已登入、帳號有效、書籍存在且有可借閱數量',[
act('進入書籍資訊頁面並點選「借閱」'),req('檢查登入狀態、帳號狀態及可借閱數量'),
fail('尚未登入','導向登入頁面'),fail('帳號無法借閱','拒絕借閱'),fail('可借閱數量為 0','顯示「目前無可借閱書籍」'),
ret('可借閱及借閱確認資訊'),show('顯示借閱確認資訊'),
('break','會員取消',[act('取消借閱'),show('結束確認；不修改任何資料')]),
act('確認借閱'),req('提交借閱確認'),m(2,2,'同一交易：建立借閱紀錄、設定日期\n可借閱數量減少 1'),
fail('建立借閱失敗／交易失敗','回復交易；紀錄與數量維持原狀'),ret('提交交易；借閱成功'),show('顯示借閱成功'),
('opt','會員選擇查看紀錄',[act('開啟「個人借閱」'),req('查詢會員借閱紀錄'),ret('借閱紀錄'),show('顯示本次借閱紀錄')])])
page('08 查看個人借閱','會員','個人借閱介面','會員已登入',[
act('點選「個人借閱」'),req('確認登入並取得目前會員 ID'),fail('未登入','導向登入頁面'),m(2,2,'依目前會員 ID 查詢借閱紀錄'),ret('借閱紀錄查詢結果'),
alt(('有借閱紀錄',[show('顯示書籍、借閱日、應歸還日、\n實際歸還日及借閱狀態')]),('無借閱紀錄',[show('顯示「目前沒有借閱紀錄」')]))])
page('09 辦理歸還','管理者','歸還書籍介面','管理者已登入；書籍有有效且尚未歸還的借閱紀錄',[
act('進入歸還書籍功能'),show('顯示條碼輸入／掃描介面'),act('掃描書籍條碼'),req('依條碼查詢書籍及有效借閱紀錄'),
fail('查無書籍','顯示「查無此書籍」'),fail('查無有效且未歸還紀錄','顯示「此書籍目前沒有借閱紀錄」'),ret('有效借閱資訊'),show('顯示借閱資訊與歸還確認'),
('break','管理者取消',[act('取消操作'),show('結束確認；不修改任何資料')]),act('確認歸還'),req('提交歸還確認'),
m(2,2,'同一交易：更新為已歸還、記錄歸還時間\n可借閱數量增加 1'),fail('歸還處理失敗','回復交易；紀錄與數量不變；顯示歸還失敗'),ret('提交交易；歸還成功'),show('顯示歸還成功')])
page('10 管理書籍','管理者','書籍管理介面','管理者已登入',[
act('進入書籍管理頁面'),req('取得書籍列表'),ret('書籍列表'),show('顯示書籍列表'),
alt(('新增書籍',[act('選擇新增'),show('顯示空白新增表單')]),('修改書籍',[act('選擇書籍並點選修改'),req('取得所選書籍資料'),ret('書籍資料'),show('顯示已填入資料的修改表單')])),
act('輸入／修改 ISBN、書名、作者、\n出版社及館藏數量；送出'),req('提交資料及操作類型'),fail('資料格式錯誤','顯示錯誤欄位'),
fail('新增且 ISBN 已存在','拒絕新增；提示 ISBN 重複'),fail('修改且 ISBN 與其他書籍重複','拒絕修改'),m(2,2,'依操作類型新增或更新書籍'),
fail('資料庫操作失敗','操作失敗；保留原資料'),ret('操作成功'),show('顯示操作成功')])
page('11 管理借閱','管理者','借閱管理介面','管理者已登入',[
act('進入借閱管理頁面'),req('取得借閱紀錄列表'),ret('借閱紀錄列表'),show('顯示借閱紀錄列表'),act('輸入會員、書籍或借閱狀態等搜尋條件'),req('查詢符合條件的借閱紀錄'),
fail('查無符合條件的紀錄','顯示「查無符合條件的借閱紀錄」'),ret('搜尋結果'),show('顯示搜尋結果'),act('選擇借閱紀錄'),req('取得所選借閱紀錄明細'),
fail('借閱紀錄不存在','顯示「借閱紀錄不存在」'),ret('借閱詳細資料'),show('顯示借閱詳細資料')],
'依原文件主要流程呈現查詢及明細，未增加未定義的修改功能。')

BASE='html=0;fontFamily=Microsoft JhengHei;fontSize=15;fontColor=#23364D;'
def size(e):
    if e[0]=='msg': return 84 if e[1]==e[2] else 64
    if e[0]=='alt': return 16+sum(44+sum(size(x) for x in items) for guard,items in e[1])
    return 60+sum(size(x) for x in e[2])
for pi,(name,actor,ui,pre,events,note) in enumerate(PAGES,1):
    total=300+sum(size(e) for e in events)
    d=ET.SubElement(DOC,'diagram',id=f'ui-{pi}',name=name)
    model=ET.SubElement(d,'mxGraphModel',grid='1',gridSize='10',page='1',pageScale='1',pageWidth='1420',pageHeight=str(total+60),arrows='1',connect='1',tooltips='1')
    root=ET.SubElement(model,'root');ET.SubElement(root,'mxCell',id='0');ET.SubElement(root,'mxCell',id='1',parent='0')
    counter=[1]
    def cell(value,style,x,y,w,h):
        counter[0]+=1
        c=ET.SubElement(root,'mxCell',id=str(counter[0]),value=value,style=BASE+style,vertex='1',parent='1')
        ET.SubElement(c,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),attrib={'as':'geometry'})
        return c
    def line(x1,y1,x2,y2,label='',dashed=False,arrow=True,selfcall=False):
        counter[0]+=1
        c=ET.SubElement(root,'mxCell',id=str(counter[0]),value=label,style=BASE+'endArrow='+(('open' if dashed else 'block') if arrow else 'none')+';endFill=1;strokeColor=#466078;strokeWidth=1.4;dashed='+str(int(dashed))+';labelBackgroundColor=#FFFFFF;verticalAlign=bottom;spacingBottom=7;',edge='1',parent='1')
        g=ET.SubElement(c,'mxGeometry',relative='1',attrib={'as':'geometry'})
        ET.SubElement(g,'mxPoint',x=str(x1),y=str(y1),attrib={'as':'sourcePoint'})
        ET.SubElement(g,'mxPoint',x=str(x2),y=str(y2),attrib={'as':'targetPoint'})
        if selfcall:
            arr=ET.SubElement(g,'Array',attrib={'as':'points'})
            for x,y in [(x1+70,y1),(x1+70,y2)]: ET.SubElement(arr,'mxPoint',x=str(x),y=str(y))
    cell(name+'｜UI 循序圖','text;align=left;fontSize=25;fontStyle=1;',40,20,1300,45)
    cell('前置條件：'+pre+'\n'+(note or '實線：操作／請求　虛線：回應／畫面呈現；break 條件成立即結束本次互動。'),'text;align=left;whiteSpace=wrap;',40,70,1320,65)
    xs=[170,670,1170]
    for x,t in zip(xs,[actor,'«boundary»\n'+ui,'«control»\n圖書管理系統']):
        cell(t,'rounded=0;whiteSpace=wrap;fillColor=#EAF2FB;strokeColor=#527CA6;',x-150,155,300,70)
        line(x,225,x,total-20,dashed=True,arrow=False)
    def render(e,y):
        typ=e[0]
        if typ=='msg':
            _,a,b,t=e
            line(xs[a],y+32,xs[b],y+60 if a==b else y+32,t,dashed=a>b,selfcall=a==b)
            return y+size(e)
        h=size(e)
        cell('','fillColor=none;strokeColor=#718399;strokeWidth=1.3;',50,y,1300,h)
        cell(typ,'fillColor=#E7EDF5;strokeColor=#718399;align=center;fontStyle=1;',50,y,70,28)
        branches=e[1] if typ=='alt' else [(e[1],e[2])]
        cy=y
        for k,(guard,items) in enumerate(branches):
            if k: line(50,cy,1350,cy,dashed=True,arrow=False)
            cell('['+guard+']','text;align=left;fontStyle=1;',135,cy+4,1180,32)
            cy+=44
            for item in items: cy=render(item,cy)
        return y+h
    y=260
    for e in events:y=render(e,y)

path=OUT/'圖書管理系統_UI循序圖.drawio'
ET.indent(DOC);ET.ElementTree(DOC).write(path,encoding='utf-8',xml_declaration=True)
parsed=ET.parse(path).getroot()
assert len(parsed)==11
for d in parsed:
    cells=d.findall('./mxGraphModel/root/mxCell')
    ids=[c.get('id') for c in cells]
    assert len(ids)==len(set(ids))
    for c in cells:
        g=c.find('mxGeometry')
        if g is not None and c.get('vertex')=='1':
            assert float(g.get('y'))+float(g.get('height'))<=float(d.find('mxGraphModel').get('pageHeight'))
print('Validated 11 draw.io sequence diagram pages.')

