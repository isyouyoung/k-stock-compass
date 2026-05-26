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
        Promise.all([loadFavorites(), loadAlerts()]).then(() => navigate(lastPage));
    } else {
        navigate('main');
    }