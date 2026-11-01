"""Analysis-class sequences derived from the source use cases, not implementation APIs."""
from pathlib import Path

source = Path(__file__).with_name('build_ui_sequences.py').read_text(encoding='utf-8-sig')
exec(source.split("BASE=")[0])

# Participant indices: actor, boundary, control, Member, Book, Borrow.
def exchange(entity,request,response):
    return [m(2,entity,request),m(entity,2,response)]

def expand(events,pi):
    result=[]
    for event in events:
        kind=event[0]
        if kind=='alt':
            result.append(alt(*[(g,expand(items,pi)) for g,items in event[1]]));continue
        if kind in ('break','opt'):
            _,guard,items=event
            prefix=[]
            if pi in (7,9) and ('交易失敗' in guard or '歸還處理失敗' in guard):
                prefix=[m(2,2,'回復整筆交易（rollback）\nBorrow 與 Book 皆恢復原狀')]
            result.append((kind,guard,prefix+expand(items,pi)));continue
        _,a,b,text=event
        if (a,b)==(2,2):
            if text=='建立會員資料':
                result+=exchange(3,'建立會員（帳號、密碼、基本資料）','建立結果');continue
            if text=='建立登入狀態並判定角色':
                result.append(m(2,2,'依會員角色建立登入狀態'));continue
            if text=='依目前會員 ID 查詢借閱紀錄':
                result+=exchange(5,'依登入會員 ID 查詢借閱紀錄','該會員借閱紀錄');continue
            if text.startswith('同一交易：'):
                result.append(m(2,2,'開始交易（任一步驟失敗即回復整筆交易）'))
                if pi==7:
                    result+=exchange(5,'建立借閱紀錄\n設定借閱日及應歸還日','建立結果')
                    result.append(('opt','前一步驟成功',exchange(4,'可借閱數量減少 1','數量更新結果')))
                else:
                    result+=exchange(5,'設定已歸還並記錄實際歸還時間','紀錄更新結果')
                    result.append(('opt','前一步驟成功',exchange(4,'可借閱數量增加 1','數量更新結果')))
                continue
            if text=='依操作類型新增或更新書籍':
                result.append(alt(('新增',exchange(4,'建立書籍資料','新增結果')),('修改',exchange(4,'更新書籍資料','更新結果'))));continue
        if (a,b)==(2,1) and text.startswith('提交交易'):
            result.append(m(2,2,'提交整筆交易（commit）'))
        result.append(event)
        if (a,b)!=(1,2): continue
        injected=[]
        if pi==1:
            injected=[m(2,2,'驗證必填欄位與資料格式')]
            injected += [('opt','資料格式正確',exchange(3,'依帳號查詢會員','帳號是否已存在'))]
        elif pi==2:
            injected=exchange(3,'依帳號取得會員','會員或不存在；密碼驗證資料、狀態、角色')+[m(2,2,'驗證密碼與帳號狀態')]
        elif pi in (3,4):
            if '驗證' in text:
                injected=[m(2,2,'驗證資料格式及可修改欄位'),('opt','資料格式正確',exchange(3,'儲存會員資料／狀態','儲存結果；失敗保留原資料'))]
            else:
                injected=exchange(3,text,'會員列表／指定會員資料；或不存在')
        elif pi in (5,6):
            injected=exchange(4,text,'書籍資料、可借阅數量及狀態；或無結果')
        elif pi==7:
            if text.startswith('檢查'):
                injected=[m(2,2,'確認目前登入狀態'),('opt','已登入',exchange(3,'查詢會員帳號狀態','有效／無法借閱')+[('opt','帳號可借閱',exchange(4,'查詢指定書籍可借閱數量','目前可借閱數量'))])]
            elif '查詢會員借閱紀錄' in text: injected=exchange(5,'依登入會員 ID 查詢借閱紀錄','會員借閱紀錄')
        elif pi==8:
            injected=[m(2,2,'確認登入狀態並取得會員 ID')]
        elif pi==9 and text.startswith('依條碼'):
            injected=exchange(4,'依條碼查詢書籍','書籍資料或不存在')+[('opt','書籍存在',exchange(5,'查詢該書籍有效且未歸還的紀錄','有效紀錄或無結果'))]
        elif pi==10:
            if text=='提交資料及操作類型':
                injected=[m(2,2,'驗證書籍資料格式'),('opt','資料格式正確',exchange(4,'檢查 ISBN 重複\n修改時排除本書籍 ID','ISBN 是否與其他書籍重複'))]
            else: injected=exchange(4,text,'書籍列表／指定書籍資料')
        elif pi==11:
            injected=exchange(5,text,'借閱列表／明細；或無結果')
        result+=injected
    return result

controls=['註冊控制','登入控制','會員資料控制','會員管理控制','書籍查詢控制','書籍查詢控制','借閱控制','個人借閱控制','歸還控制','書籍管理控制','借閱管理控制']
PAGES=[(name,actor,ui,pre,expand(events,i),note) for i,(name,actor,ui,pre,events,note) in enumerate(PAGES,1)]

# Reuse the editable draw.io layout engine with an expanded BCE participant set.
renderer='BASE='+source.split('BASE=',1)[1]
renderer=renderer.replace("id=f'ui-{pi}'", "id=f'ac-{pi}'")
renderer=renderer.replace("pageWidth='1420'", "pageWidth='2760'")
renderer=renderer.replace("name+'｜UI 循序圖'", "name+'｜AC 分析類別循序圖'")
renderer=renderer.replace("40,20,1300,45", "40,20,2620,45").replace("40,70,1320,65", "40,70,2640,65")
renderer=renderer.replace("(note or '實線：操作／請求　虛線：回應／畫面呈現；break 條件成立即結束本次互動。')", "'分析層級：Boundary／Control／Entity；實體互動為概念責任，非既有程式 API。break 成立即結束本次互動。 '+note")
renderer=renderer.replace("xs=[170,670,1170]", "xs=[190,650,1110,1570,2030,2490]")
renderer=renderer.replace("[actor,'«boundary»\\n'+ui,'«control»\\n圖書管理系統']", "[actor,'«boundary»\\n'+ui,'«control»\\n'+controls[pi-1],'«entity»\\n會員 Member','«entity»\\n書籍 Book','«entity»\\n借閱紀錄 Borrow']")
renderer=renderer.replace('50,y,1300,h', '50,y,2660,h').replace('50,cy,1350,cy','50,cy,2710,cy').replace('135,cy+4,1180,32','135,cy+4,2530,32')
renderer=renderer.replace('圖書管理系統_UI循序圖.drawio','圖書管理系統_AC循序圖.drawio')
renderer=renderer.replace('可借阅','可借閱')
exec(renderer)
