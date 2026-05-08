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
    state.currentPage = page;
    Object.assign(state, params);
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
        loadFavorites().then(() => navigate('main'));
    } else {
        navigate('main');
    }