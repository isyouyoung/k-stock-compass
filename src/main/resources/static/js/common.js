// ══════════════════════════════════════
// 상태 관리
// ══════════════════════════════════════
const state = {
    loggedIn: false,
    user: null,
    currentPage: 'login',
    favorites: [],
    alerts: [],
    alertLogs: [],
    currentStock: null,
    editAlertId: null,
    delFavCode: null,
    delAlertId: null,
    editPostId: null,
    editPostTitle: '',
    editPostContent: '',
};

// ══════════════════════════════════════
// 유틸
// ══════════════════════════════════════
function fmt(n){return Number(n).toLocaleString();}
function updown(v){return v>=0?`<span class="up">▲ +${fmt(Math.abs(v))}</span>`:`<span class="down">▼ -${fmt(Math.abs(v))}</span>`;}
function updownPct(v){return v>=0?`<span class="up">(+${Math.abs(v).toFixed(2)}%)</span>`:`<span class="down">(-${Math.abs(v).toFixed(2)}%)</span>`;}
function showToast(msg,duration=2500){
    const t=document.getElementById('toast');
    t.textContent=msg; t.style.display='block';
    setTimeout(()=>t.style.display='none',duration);
}
function openModal(id){document.getElementById(id).classList.add('open');}
function closeModal(id){document.getElementById(id).classList.remove('open');}

// ══════════════════════════════════════
// 라우터
// ══════════════════════════════════════
function navigate(page, params={}) {
    // 페이지 이동 시 기존 인터벌 전부 정리
    if(window._stockDetailInterval) {
        clearInterval(window._stockDetailInterval);
        window._stockDetailInterval = null;
    }
    if(window._mainFavInterval) {
        clearInterval(window._mainFavInterval);
        window._mainFavInterval = null;
    }
    if(window._stockIndexInterval) {
        clearInterval(window._stockIndexInterval);
        window._stockIndexInterval = null;
    }
    if(window._stockSearchInterval) {
        clearInterval(window._stockSearchInterval);
        window._stockSearchInterval = null;
    }
    if(window._favListInterval) {
        clearInterval(window._favListInterval);
        window._favListInterval = null;
    }
    if(window._portfolioInterval) {
        clearInterval(window._portfolioInterval);
        window._portfolioInterval = null;
    }
    if(window._assetSaveInterval) {
        clearInterval(window._assetSaveInterval);
        window._assetSaveInterval = null;
    }
    if(window._assetChartInterval) {
        clearInterval(window._assetChartInterval);
        window._assetChartInterval = null;
    }
    state.currentPage = page;
    Object.assign(state, params);
    // 현재 페이지 저장 (새로고침 복원용)
    if(!['login','signup','signup_done','find_id','find_pw','find_pw_reset'].includes(page)) {
        sessionStorage.setItem('lastPage', page);
        if(page === 'stock_detail' && state.currentStock) {
            sessionStorage.setItem('lastStock', state.currentStock);
        }
    }
    if (['login', 'signup', 'signup_done', 'find_id', 'find_pw', 'find_pw_reset'].includes(page)) {
        document.getElementById('app').style.display = 'none';
        document.getElementById('authArea').style.display = 'block';
        renderAuth(page);
    } else {
        document.getElementById('app').style.display = 'flex';
        document.getElementById('authArea').style.display = 'none';
        renderSidebar();
        renderPage(page);
    }
    window.scrollTo(0, 0);
}

// ══════════════════════════════════════
// 알림 체크 & 브라우저 알림
// ══════════════════════════════════════

let _lastUnreadCount = 0;
let _tabBlinkInterval = null;

// 탭 깜빡임 시작
function startTabBlink() {
    if (_tabBlinkInterval) return; // 이미 깜빡이는 중이면 무시
    let blink = true;
    _tabBlinkInterval = setInterval(() => {
        document.title = blink ? '🔔 새 알림!' : 'K-Stock Compass';
        blink = !blink;
    }, 1000);
}

// 탭 깜빡임 중지
function stopTabBlink() {
    if (_tabBlinkInterval) {
        clearInterval(_tabBlinkInterval);
        _tabBlinkInterval = null;
    }
    document.title = 'K-Stock Compass';
}

// 브라우저 푸시 알림 권한 요청
function requestNotificationPermission() {
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }
}

// 브라우저 푸시 알림 발송
function sendBrowserNotification(msg) {
    if ('Notification' in window && Notification.permission === 'granted') {
        const notification = new Notification('📬 K-Stock Compass 알림', {
            body: msg,
            icon: '/favicon.ico'
        });
        // 알림 클릭 시 창 포커스 + 알림 내역 페이지로 이동
        notification.onclick = () => {
            window.focus();
            navigate('alert_log');
        };
        // 5초 후 자동 닫기
        setTimeout(() => notification.close(), 5000);
    }
}

// 30초마다 새 알림 체크
function startAlertPolling() {
    if (window._alertCheckInterval) return;
    window._alertCheckInterval = setInterval(async () => {
        if (!state.loggedIn) return;
        try {
            await loadAlertLogs();
            const unread = state.alertLogs.filter(l => !l.read).length;

            // 새 알림이 생겼을 때만 처리
            if (unread > _lastUnreadCount) {
                const newCount = unread - _lastUnreadCount;

                // 1. 탭 깜빡이기
                startTabBlink();

                // 2. 토스트 메시지
                showToast(`📬 읽지 않은 알림이 ${unread}개 있습니다!`, 4000);

                // 3. 브라우저 푸시 알림
                const latestLog = state.alertLogs.filter(l => !l.read).slice(-1)[0];
                if (latestLog) {
                    sendBrowserNotification(latestLog.msg);
                }

                // 4. 사이드바 갱신
                if (state.loggedIn) renderSidebar();
            }

            // 모두 읽으면 깜빡임 중지
            if (unread === 0) {
                stopTabBlink();
            }

            _lastUnreadCount = unread;

        } catch(e) {
            console.error('알림 체크 실패', e);
        }
    }, 30000);
}

// ══════════════════════════════════════
// 초기 실행
// ══════════════════════════════════════
document.getElementById('authArea').style.display='block';

// JWT 복원
const savedToken = localStorage.getItem('jwt');
const savedEmail = localStorage.getItem('userEmail');
if(savedToken && savedEmail){
    state.loggedIn = true;
    state.user = { email: savedEmail, nickname: savedEmail.split('@')[0] };
    const lastPage = sessionStorage.getItem('lastPage') || 'main';
    if(lastPage === 'stock_detail') {
        const lastStock = sessionStorage.getItem('lastStock');
        if(lastStock) state.currentStock = lastStock;
    }
    Promise.all([loadFavorites(), loadAlerts(), loadAlertLogs()]).then(() => {
        navigate(lastPage);
        requestNotificationPermission(); // 푸시 알림 권한 요청
        startAlertPolling(); // 알림 폴링 시작
    });
} else {
    navigate('main');
}

// ══════════════════════════════════════
// 자유게시판 API 및 페이지
// ══════════════════════════════════════

// 1. 게시글 목록 API 호출
async function fetchBoardPosts() {
    try {
        const res = await fetch('/api/board');
        if (!res.ok) throw new Error('목록 로드 실패');
        return await res.json();
    } catch (e) {
        console.error('게시판 목록 로드 실패:', e);
        return [];
    }
}

// 게시글 수정 모드 진입 (인코딩된 값을 디코딩하여 상태 저장)
function editPost(postId, encodedTitle, encodedContent) {
    state.editPostId = postId;
    state.editPostTitle = decodeURIComponent(encodedTitle);
    state.editPostContent = decodeURIComponent(encodedContent);
    navigate('board_write');
}

// 게시글 저장 (등록 및 수정 공용)
async function doSavePost() {
    const title = document.getElementById('boardTitle').value.trim();
    const content = document.getElementById('boardContent').value.trim();

    if (!title || !content) {
        showToast('제목과 내용을 모두 입력해 주세요.');
        return;
    }

    const isEdit = !!state.editPostId;
    const url = isEdit ? `/api/board/${state.editPostId}` : '/api/board';
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const res = await authFetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, content })
        });

        if (res.ok) {
            showToast(isEdit ? '게시글이 수정되었습니다! ✏️' : '게시글이 등록되었습니다! 📝');
            state.editPostId = null; // 수정 모드 초기화
            navigate('board');
        } else {
            const err = await res.text();
            showToast(err || '처리 중 오류가 발생했습니다.');
        }
    } catch (e) {
        showToast('서버 오류가 발생했습니다.');
    }
}

// 3. 게시글 삭제 API 호출
async function doDeletePost(postId) {
    if (!confirm('정말 이 게시글을 삭제하시겠습니까?')) return;

    try {
        const res = await authFetch(`/api/board/${postId}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('게시글이 삭제되었습니다.');
            navigate('board');
        } else {
            const err = await res.text();
            showToast(err || '삭제 권한이 없거나 실패했습니다.');
        }
    } catch (e) {
        showToast('서버 오류가 발생했습니다.');
    }
}

// 4. 자유게시판 목록 페이지 UI
// 4. 자유게시판 목록 페이지 UI
function pgBoard() {
    fetchBoardPosts().then(posts => {
        const container = document.getElementById('boardListContainer');
        if (!container) return;

        if (posts.length === 0) {
            container.innerHTML = `
                <div class="card" style="padding:40px;text-align:center;">
                    <div style="font-size:36px;margin-bottom:12px;">💬</div>
                    <div style="font-size:16px;font-weight:700;color:var(--navy);margin-bottom:6px;">등록된 게시글이 없습니다</div>
                    <div style="font-size:13px;color:var(--gray);">첫 번째 이야기를 자유게시판에 남겨보세요!</div>
                </div>`;
            return;
        }

        container.innerHTML = posts.map(p => `
            <div class="card mb12" style="padding:20px 24px;">
                <div class="flex flex-between flex-center mb8">
                    <span style="font-size:16px;font-weight:700;color:var(--navy);">${p.title}</span>
                    <span style="font-size:12px;color:var(--gray);">${p.regDt}</span>
                </div>
                <div style="font-size:14px;color:var(--dark);line-height:1.6;margin-bottom:12px;white-space:pre-wrap;">${p.content}</div>
                <div class="flex flex-between flex-center" style="border-top:1px solid var(--border);padding-top:10px;margin-top:8px;">
                    <span style="font-size:12px;color:var(--blue);font-weight:600;">✍️ ${p.authorName}</span>
                    ${state.loggedIn && state.user?.email === p.authorEmail ? `
                        <div style="display:flex;gap:6px;">
                            <button class="btn btn-sm btn-outline" style="padding:4px 10px;" onclick="editPost(${p.postId}, '${encodeURIComponent(p.title)}', '${encodeURIComponent(p.content)}')">수정</button>
                            <button class="btn btn-sm" style="background:#FFF5F5;color:var(--red-err);border:1px solid var(--red-err);padding:4px 10px;" onclick="doDeletePost(${p.postId})">삭제</button>
                        </div>
                    ` : ''}
                </div>
            </div>
        `).join('');
    });

    return `
        <div class="page-wrap">
            <div class="flex flex-between flex-center mb24">
                <div class="page-title">🗣️ 자유게시판</div>
                ${state.loggedIn ? `
                    <button class="btn btn-primary" onclick="state.editPostId=null;navigate('board_write')">✏️ 글쓰기</button>
                ` : `
                    <button class="btn btn-secondary btn-sm" onclick="openModal('modalNeedLogin')">🔒 글쓰기는 로그인 필요</button>
                `}
            </div>
            <div id="boardListContainer">
                <div class="card" style="padding:40px;text-align:center;color:var(--gray);">게시글을 불러오는 중입니다...</div>
            </div>
        </div>`;
}

// 5. 게시글 작성 페이지 UI
function pgBoardWrite() {
    const isEdit = !!state.editPostId;
    const title = isEdit ? state.editPostTitle : '';
    const content = isEdit ? state.editPostContent : '';

    return `
        <div class="page-wrap">
            <div class="page-header"><div class="page-title">${isEdit ? '✏️ 게시글 수정' : '✏️ 게시글 작성'}</div></div>
            <div style="display:flex;justify-content:center;">
                <div class="card" style="width:100%;max-width:680px;">
                    <div class="form-group">
                        <div class="label">제목 <span class="req">*</span></div>
                        <input class="input" id="boardTitle" 
                               value="${title.replace(/"/g, '&quot;')}" 
                               placeholder="제목을 입력하세요">
                    </div>
                    <div class="form-group">
                        <div class="label">내용 <span class="req">*</span></div>
                        <textarea class="input" id="boardContent" 
                                  style="height:180px;resize:vertical;padding:12px;" 
                                  placeholder="자유롭게 이야기를 작성해주세요.">${content}</textarea>
                    </div>
                    <div style="display:flex;gap:10px;margin-top:20px;">
                        <button class="btn btn-primary flex-1" onclick="doSavePost()">${isEdit ? '수정 완료' : '등록하기'}</button>
                        <button class="btn btn-secondary" onclick="state.editPostId=null;navigate('board')">취소</button>
                    </div>
                </div>
            </div>
        </div>`;
}