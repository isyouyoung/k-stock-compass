// ══════════════════════════════════════
// 상태 관리
// ══════════════════════════════════════
const state = {
    loggedIn: false,
    user: null,
    currentPage: 'login',
    favorites: [
        {code:'005930',name:'삼성전자',market:'KOSPI',price:61200,change:1200,pct:2.00},
        {code:'000660',name:'SK하이닉스',market:'KOSPI',price:182500,change:-1500,pct:-0.81},
        {code:'035420',name:'NAVER',market:'KOSPI',price:215000,change:3000,pct:1.41},
        {code:'035720',name:'카카오',market:'KOSDAQ',price:45800,change:-200,pct:-0.43},
    ],
    alerts: [
        {id:1,code:'005930',name:'삼성전자',target:70000,direction:'이상',current:61200},
        {id:2,code:'000660',name:'SK하이닉스',target:200000,direction:'이상',current:182500},
        {id:3,code:'035420',name:'NAVER',target:180000,direction:'이하',current:215000},
    ],
    alertLogs: [
        {id:1,name:'삼성전자',msg:'목표가 70,000원 이상 도달!',time:'2026.03.24 14:32',read:false},
        {id:2,name:'SK하이닉스',msg:'목표가 200,000원 이상 도달!',time:'2026.03.23 09:15',read:false},
        {id:3,name:'NAVER',msg:'목표가 180,000원 이하 도달!',time:'2026.03.22 15:48',read:true},
        {id:4,name:'카카오',msg:'목표가 50,000원 이상 도달!',time:'2026.03.21 11:20',read:true},
    ],
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
    }

    navigate('main');