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