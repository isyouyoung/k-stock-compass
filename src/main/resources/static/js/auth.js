function renderSidebar(){
    const navItems = [
        {icon:'🏠',label:'메인',page:'main'},
        {icon:'📊',label:'시장 지수',page:'stock_index'},
        {icon:'🔍',label:'종목 검색',page:'stock_search'},
        {icon:'⭐',label:'관심종목',page:'fav_list',auth:true},
        {icon:'🔔',label:'알림',page:'alert_list',auth:true},
        {icon:'👤',label:'마이페이지',page:'mypage',auth:true},
    ];
    document.getElementById('navSection').innerHTML = navItems.map(item=>`
        <div class="nav-item ${state.currentPage===item.page?'active':''}"
        onclick="${!state.loggedIn&&item.auth?`openModal('modalNeedLogin')`:`navigate('${item.page}')`}">
        <span class="nav-icon">${item.icon}</span>${item.label}
        </div>
        `).join('') + (state.loggedIn?`
        <div class="nav-item" onclick="openModal('modalLogout')" style="margin-top:8px;">
        <span class="nav-icon">🚪</span>로그아웃
        </div>`:'');
    document.getElementById('userInfo').innerHTML = state.loggedIn
        ? `<div class="user-avatar">👤</div><div><div class="user-name">${state.user.nickname}</div><div class="user-email">${state.user.email}</div></div>`
        : `<button class="btn btn-primary btn-sm btn-full" onclick="navigate('login')">로그인</button>`;
}

// ══════════════════════════════════════
// 인증 화면
// ══════════════════════════════════════
function renderAuth(page){
    const area = document.getElementById('authArea');
    const pages = {
        login: renderLogin,
        signup: renderSignup,
        signup_done: renderSignupDone,
        find_id: renderFindId,
        find_pw: renderFindPw,
        find_pw_reset: renderFindPwReset,
    };
    area.innerHTML = (pages[page]||renderLogin)();
}

const LOGO_SVG = `<div class="auth-logo-fixed" onclick="navigate('main')" style="cursor:pointer;z-index:10;"><svg width="180" viewBox="0 0 168 66" xmlns="http://www.w3.org/2000/svg">
        <rect width="168" height="66" fill="white" rx="10"/>
        <rect x="12" y="7" width="22" height="52" rx="5" fill="#1E3A5F"/>
        <rect x="12" y="7" width="22" height="52" rx="5" fill="none" stroke="#2E75B6" stroke-width="0.7"/>
        <rect x="14" y="4" width="18" height="5" rx="2.5" fill="#162D4A"/>
        <rect x="15" y="10" width="16" height="44" rx="3" fill="#0F2340"/>
        <circle cx="23" cy="21" r="5.5" fill="#7F1D1D"/><circle cx="23" cy="21" r="4" fill="#DC2626"/>
        <circle cx="23" cy="35" r="5.5" fill="#713F12"/><circle cx="23" cy="35" r="4" fill="#CA8A04"/>
        <circle cx="23" cy="49" r="7" fill="#22C55E" opacity="0.15"/>
        <circle cx="23" cy="49" r="5.5" fill="#14532D"/><circle cx="23" cy="49" r="4" fill="#16A34A"/>
        <circle cx="23" cy="49" r="4" fill="none" stroke="#4ADE80" stroke-width="0.8" opacity="0.7"/>
        <circle cx="20" cy="46" r="1.5" fill="#BBF7D0" opacity="0.6"/>
        <rect x="20" y="59" width="6" height="5" rx="1.5" fill="#1E3A5F"/>
        <rect x="15" y="62" width="16" height="3" rx="1.5" fill="#1E3A5F"/>
        <text x="42" y="28" font-family="'Noto Sans KR',Arial,sans-serif" font-size="22" font-weight="900" fill="#1F4E79" letter-spacing="-0.5">K-Stock</text>
        <text x="42" y="52" font-family="'Noto Sans KR',Arial,sans-serif" font-size="22" font-weight="900" fill="#2E75B6" letter-spacing="-0.5">Compass</text>
        </svg></div>`;

function authLayout(formHtml){
    return `<div class="auth-layout">
        <div class="auth-form-wrap">${LOGO_SVG}${formHtml}</div>
        <div class="auth-brand">
        <div class="auth-brand-title">K-Stock<br>Compass</div>
        <div class="auth-brand-sub">재무 신호등으로<br>기업을 한눈에</div>
        </div>
        </div>`;
}

function renderLogin(){return authLayout(`
        <div class="auth-title">로그인</div>
        <div class="auth-sub">K-Stock Compass와 함께 주식을 분석하세요</div>
        <div class="form-group"><div class="label">이메일 <span class="req">*</span></div>
        <input class="input" id="loginEmail" placeholder="mail@example.com" oninput="clearErr('loginEmailErr')">
        <div class="error-msg" id="loginEmailErr"></div></div>
        <div class="form-group"><div class="label">비밀번호 <span class="req">*</span></div>
        <input class="input" id="loginPw" type="password" placeholder="비밀번호" oninput="clearErr('loginPwErr')" onkeydown="if(event.key==='Enter')doLogin()">
        <div class="error-msg" id="loginPwErr"></div></div>
        <div id="loginErr" class="warning mb12" style="display:none;"></div>
        <button class="btn btn-primary btn-full mb8" onclick="doLogin()">로그인</button>
        <button class="btn btn-secondary btn-full mb16" onclick="navigate('signup')">회원 가입</button>
        <div style="display:flex;gap:16px;justify-content:center;">
        <span class="auth-link" style="font-size:12px;color:var(--blue);cursor:pointer;" onclick="navigate('find_id')">아이디 찾기</span>
        <span style="color:var(--border);">|</span>
        <span class="auth-link" style="font-size:12px;color:var(--blue);cursor:pointer;" onclick="navigate('find_pw')">비밀번호 찾기</span>
        </div>
        `);}

function renderSignup(){return authLayout(`
        <div class="auth-title">회원가입</div>
        <div class="auth-sub">K-Stock Compass에 오신 것을 환영합니다</div>
        <div class="form-group"><div class="label">이메일 <span class="req">*</span></div>
        <div style="display:flex;gap:8px;">
        <input class="input" id="regEmail" placeholder="mail@example.com" style="flex:1;">
        <button class="btn btn-blue btn-sm" style="white-space:nowrap;" onclick="sendVerifyCode()">인증번호 발송</button>
        </div>
        <div class="error-msg" id="regEmailErr"></div>
        </div>
        <div class="form-group" id="verifyGroup" style="display:none;">
        <div class="label">인증번호 <span class="req">*</span></div>
        <div style="display:flex;gap:8px;">
        <input class="input" id="regVerifyCode" placeholder="인증번호 6자리 입력" style="flex:1;">
        <button class="btn btn-blue btn-sm" style="white-space:nowrap;" onclick="checkVerifyCode()">확인</button>
        </div>
        <div class="error-msg" id="regVerifyErr"></div>
        <div id="regVerifyOk" style="font-size:11px;color:var(--green);margin-top:4px;display:none;">✓ 이메일 인증이 완료되었습니다.</div>
        </div>
        <div class="form-group"><div class="label">이름 <span class="req">*</span></div>
        <input class="input" id="regNick" placeholder="이름을 입력하세요 (2~10자)">
        <div class="error-msg" id="regNickErr"></div></div>
        <div class="form-group"><div class="label">전화번호 <span class="req">*</span></div>
        <input class="input" id="regPhone" placeholder="01012345678" autocomplete="off">
        <div class="error-msg" id="regPhoneErr"></div>
        </div>
        <div class="form-group"><div class="label">비밀번호 <span class="req">*</span></div>
        <input class="input" id="regPw" type="password" placeholder="8자 이상, 영문+숫자+특수문자" autocomplete="new-password">
        <div class="error-msg" id="regPwErr"></div></div>
        <div class="form-group"><div class="label">비밀번호 확인 <span class="req">*</span></div>
        <input class="input" id="regPw2" type="password" placeholder="비밀번호를 다시 입력하세요" autocomplete="new-password">
        <div class="error-msg" id="regPw2Err"></div></div>
        <button class="btn btn-primary btn-full mb8" onclick="doSignup()">회원가입</button>
        <div style="text-align:center;font-size:12px;color:var(--gray);margin-top:10px;">이미 계정이 있으신가요? <span style="color:var(--blue);cursor:pointer;" onclick="navigate('login')">로그인</span></div>
        `);}

function renderSignupDone(){return `
        <div class="success-wrap" style="min-height:100vh;">
        <div class="success-card">
        <div class="success-circle">✓</div>
        <div style="font-size:26px;font-weight:700;color:var(--navy);margin-bottom:10px;">회원가입이 완료되었습니다!</div>
        <div style="font-size:14px;color:var(--gray);margin-bottom:32px;">K-Stock Compass의 모든 기능을 이용하실 수 있습니다.</div>
        <button class="btn btn-primary btn-full mb8" onclick="navigate('login')">로그인 하러 가기</button>
        <button class="btn btn-secondary btn-full" onclick="navigate('main')">메인으로</button>
        </div>
        </div>`;}

function renderFindId(){return authLayout(`
        <div class="auth-title">아이디 찾기</div>
        <div class="auth-sub" style="margin-bottom:28px;">가입 시 등록한 이름과 전화번호로 이메일을 찾을 수 있습니다.</div>
        <div class="form-group"><div class="label">이름 <span class="req">*</span></div>
        <input class="input" id="findName" placeholder="가입 시 등록한 이름 입력">
        <div class="error-msg" id="findNameErr"></div></div>
        <div class="form-group"><div class="label">전화번호 <span class="req">*</span></div>
        <input class="input" id="findPhone" placeholder="01012345678">
        <div class="error-msg" id="findPhoneErr"></div></div>
        <button class="btn btn-primary btn-full mb8" onclick="doFindId()">아이디 찾기</button>
        <div class="divider"></div>
        <button class="btn btn-secondary btn-full mb8" onclick="navigate('login')">로그인 화면으로 돌아가기</button>
        <button class="btn btn-outline btn-full" onclick="navigate('find_pw')">비밀번호 찾기</button>
        `);}

function renderFindPw(){return authLayout(`
        <div class="auth-title">비밀번호 찾기</div>
        <div class="auth-sub" style="margin-bottom:28px;">가입 시 등록한 이름과 이메일로 임시 비밀번호를 받을 수 있습니다.</div>
        <div class="form-group"><div class="label">이름 <span class="req">*</span></div>
        <input class="input" id="findPwName" placeholder="가입 시 등록한 이름 입력">
        <div class="error-msg" id="findPwNameErr"></div></div>
        <div class="form-group"><div class="label">이메일 <span class="req">*</span></div>
        <input class="input" id="findEmail" placeholder="가입 시 사용한 이메일 입력">
        <div class="error-msg" id="findEmailErr"></div></div>
        <button class="btn btn-primary btn-full mb12" onclick="doSendReset()">임시 비밀번호 발송</button>
        <button class="btn btn-secondary btn-full" onclick="navigate('login')">로그인 화면으로 돌아가기</button>
        `);}

function renderFindPwReset(){return authLayout(`
        <div class="auth-title">새 비밀번호 설정</div>
        <div class="auth-sub" style="margin-bottom:28px;">이메일 링크를 통해 새 비밀번호를 설정합니다.</div>
        <div class="form-group"><div class="label">새 비밀번호 <span class="req">*</span></div>
        <input class="input" id="newPw" type="password" placeholder="8자 이상, 영문+숫자+특수문자" oninput="updatePwStrength()"></div>
        <div class="pw-strength" id="pwStrength">
        <div class="pw-bar" id="pb1"></div><div class="pw-bar" id="pb2"></div><div class="pw-bar" id="pb3"></div>
        <span style="font-size:12px;color:var(--gray);" id="pwStrengthLabel">비밀번호 강도</span>
        </div>
        <div class="form-group"><div class="label">새 비밀번호 확인 <span class="req">*</span></div>
        <input class="input" id="newPw2" type="password" placeholder="비밀번호를 다시 입력하세요"></div>
        <button class="btn btn-primary btn-full" onclick="doPwReset()">비밀번호 변경 완료</button>
        `);}

// ══════════════════════════════════════
// 액션 함수들
// ══════════════════════════════════════
function clearErr(id){const el=document.getElementById(id);if(el)el.textContent='';}

async function doLogin(){
    const email = document.getElementById('loginEmail').value.trim();
    const pw = document.getElementById('loginPw').value;

    if(!email){ document.getElementById('loginEmailErr').textContent='이메일을 입력해 주세요.'; return; }
    if(!/\S+@\S+\.\S+/.test(email)){ document.getElementById('loginEmailErr').textContent='올바른 이메일 형식이 아닙니다.'; return; }
    if(!pw){ document.getElementById('loginPwErr').textContent='비밀번호를 입력해 주세요.'; return; }

    try {
        const res = await fetch('/api/user/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({userEmail: email, userPwd: pw})
        });

        if(res.ok){
            const data = await res.json(); // text() → json()으로 변경
            localStorage.setItem('jwt', data.accessToken); // accessToken 저장
            localStorage.setItem('refreshToken', data.refreshToken); // refreshToken도 저장
            localStorage.setItem('userEmail', email);
            state.loggedIn = true;
            state.user = {email: email, nickname: email.split('@')[0]};
            document.getElementById('loginSuccessMsg').textContent = `환영합니다, ${email.split('@')[0]}님!`;
            openModal('modalLoginSuccess');
            setTimeout(() => {
                document.getElementById('loginSuccessBtn').focus();
            }, 100);
            requestNotificationPermission();
            startAlertPolling();
        } else {
            openModal('modalLoginFail');
        }
    } catch(e) {
        openModal('modalLoginFail');
    }
}

async function sendVerifyCode(){
    const email = document.getElementById('regEmail').value.trim();
    const errEl = document.getElementById('regEmailErr');

    if(!email){ errEl.textContent='이메일을 입력해 주세요.'; errEl.style.color='var(--red-err)'; return; }
    if(!/\S+@\S+\.\S+/.test(email)){ errEl.textContent='올바른 이메일 형식이 아닙니다.'; errEl.style.color='var(--red-err)'; return; }

    try {
        const res = await fetch(`/api/user/check-email?userEmail=${encodeURIComponent(email)}`);
        const isDuplicate = await res.json(); // true/false 받아서
        if(isDuplicate){
            errEl.textContent='이미 사용 중인 이메일입니다.';
            errEl.style.color='var(--red-err)';
            return;
        }

        const sendRes = await fetch('/api/user/send-code', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({userEmail: email})
        });

        if(sendRes.ok){
            errEl.textContent='사용 가능한 이메일입니다.';
            errEl.style.color='var(--green)';
            state._emailVerified = false;
            document.getElementById('verifyGroup').style.display='block';
            document.getElementById('regVerifyErr').textContent='인증번호를 발송했습니다. 이메일을 확인해 주세요.';
            document.getElementById('regVerifyErr').style.color='var(--blue)';
            document.getElementById('regVerifyOk').style.display='none';
        } else {
            errEl.textContent='인증번호 발송에 실패했습니다.';
            errEl.style.color='var(--red-err)';
        }
    } catch(e) {
        errEl.textContent='서버 오류가 발생했습니다.';
        errEl.style.color='var(--red-err)';
    }
}

async function checkVerifyCode(){
    const email = document.getElementById('regEmail').value.trim();
    const code = document.getElementById('regVerifyCode').value.trim();
    const vErr = document.getElementById('regVerifyErr');

    if(!code){ vErr.textContent='인증번호를 입력해 주세요.'; vErr.style.color='var(--red-err)'; return; }

    try {
        const res = await fetch('/api/user/verify-code', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({userEmail: email, verifyCode: code})
        });

        if(res.ok){
            state._emailVerified = true;
            document.getElementById('regVerifyOk').style.display='block';
            vErr.textContent='';
        } else {
            vErr.textContent='인증번호가 일치하지 않습니다.';
            vErr.style.color='var(--red-err)';
            state._emailVerified = false;
        }
    } catch(e) {
        vErr.textContent='서버 오류가 발생했습니다.';
        vErr.style.color='var(--red-err)';
    }
}


function validateSignup(){
    const email=document.getElementById('regEmail')?.value||'';
    const pw=document.getElementById('regPw')?.value||'';
    const pw2=document.getElementById('regPw2')?.value||'';
    const nick=document.getElementById('regNick')?.value||'';
    const phone=document.getElementById('regPhone')?.value||'';
    const agree=document.getElementById('regAgree')?.checked||false;
    let valid=true;
    if(pw&&pw.length<8){document.getElementById('regPwErr').textContent='8자 이상 입력해 주세요.';valid=false;}
    else if(document.getElementById('regPwErr'))document.getElementById('regPwErr').textContent='';
    if(pw2&&pw&&pw!==pw2){document.getElementById('regPw2Err').textContent='비밀번호가 일치하지 않습니다.';valid=false;}
    else if(document.getElementById('regPw2Err'))document.getElementById('regPw2Err').textContent='';
    const btn=document.getElementById('regBtn');
    if(btn){
        const verified=state._emailVerified||false;
        const allFilled=email&&verified&&pw&&pw2&&nick&&phone&&agree&&valid&&pw===pw2&&pw.length>=8;
        btn.style.background=allFilled?'var(--navy)':'var(--light-gray)';
        btn.style.color=allFilled?'white':'var(--gray)';
        btn.style.cursor=allFilled?'pointer':'not-allowed';
    }
}


async function doSignup(){
    const email = document.getElementById('regEmail').value.trim();
    const verified = state._emailVerified || false;
    const nick = document.getElementById('regNick').value.trim();
    const phone = document.getElementById('regPhone').value.trim();
    const pw = document.getElementById('regPw').value;
    const pw2 = document.getElementById('regPw2').value;

    if(!email){ document.getElementById('regEmailErr').textContent='이메일을 입력해 주세요.'; return; }
    if(!verified){ document.getElementById('regVerifyErr').textContent='이메일 인증을 완료해 주세요.'; return; }
    if(!nick){ document.getElementById('regNickErr').textContent='이름을 입력해 주세요.'; return; }
    if(!phone){ document.getElementById('regPhoneErr').textContent='전화번호를 입력해 주세요.'; return; }
    if(!pw){ document.getElementById('regPwErr').textContent='비밀번호를 입력해 주세요.'; return; }
    if(pw.length<8){ document.getElementById('regPwErr').textContent='8자 이상 입력해 주세요.'; return; }

// 비밀번호 패턴 검증 (영문+숫자+특수문자)
    const pwPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{4,20}$/;
    if(!pwPattern.test(pw)){
        document.getElementById('regPwErr').textContent='영문, 숫자, 특수문자(@$!%*#?&)를 포함하여 입력해 주세요.';
        return;
    }
    if(pw!==pw2){ document.getElementById('regPw2Err').textContent='비밀번호가 일치하지 않습니다.'; return; }

    try {
        const res = await fetch('/api/user/signup', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                userEmail: email,
                userPwd: pw,
                userName: nick,
                userPnum: phone
            })
        });

        if(res.ok){
            openModal('modalSignupDone');
        } else {
            const err = await res.text();
            console.log('회원가입 실패:', err);
            if(err.includes('전화번호')) {
                document.getElementById('regPhoneErr').textContent = err;
                document.getElementById('regPhoneErr').style.color = 'var(--red-err)';
            } else if(err.includes('이메일')) {
                document.getElementById('regEmailErr').textContent = err;
                document.getElementById('regEmailErr').style.color = 'var(--red-err)';
            } else {
                showToast('회원가입 실패: ' + err);
            }
        }
    } catch(e) {
        showToast('서버 오류가 발생했습니다.');
    }
}

async function doFindId(){
    const name = document.getElementById('findName').value.trim();
    const phone = document.getElementById('findPhone').value.trim();
    const nameErr = document.getElementById('findNameErr');
    const phoneErr = document.getElementById('findPhoneErr');

    if(!name){ nameErr.textContent='이름을 입력해 주세요.'; nameErr.style.color='var(--red-err)'; document.getElementById('findName').focus(); return; }
    nameErr.textContent='';
    if(!phone){ phoneErr.textContent='전화번호를 입력해 주세요.'; phoneErr.style.color='var(--red-err)'; document.getElementById('findPhone').focus(); return; }
    phoneErr.textContent='';

    try {
        const res = await fetch(`/api/user/find-email?userName=${encodeURIComponent(name)}&userPnum=${encodeURIComponent(phone)}`);

        if(res.ok){
            const maskedEmail = await res.text();
            document.getElementById('findIdResultLabel').textContent = `이름 '${name}'으로 등록된 계정`;
            document.getElementById('findIdResultEmail').textContent = maskedEmail;
            openModal('modalFindIdResult');
        } else {
            openModal('modalFindIdFail');
        }
    } catch(e) {
        openModal('modalFindIdFail');
    }
}

async function doSendReset(){
    const name = document.getElementById('findPwName').value.trim();
    const email = document.getElementById('findEmail').value.trim();
    const nameErr = document.getElementById('findPwNameErr');
    const emailErr = document.getElementById('findEmailErr');

    if(!name){ nameErr.textContent='이름을 입력해 주세요.'; nameErr.style.color='var(--red-err)'; document.getElementById('findPwName').focus(); return; }
    nameErr.textContent='';
    if(!email){ emailErr.textContent='이메일을 입력해 주세요.'; emailErr.style.color='var(--red-err)'; document.getElementById('findEmail').focus(); return; }
    if(!/\S+@\S+\.\S+/.test(email)){ emailErr.textContent='올바른 이메일 형식이 아닙니다.'; emailErr.style.color='var(--red-err)'; return; }
    emailErr.textContent='';

    try {
        const res = await fetch(`/api/user/reset-password?userName=${encodeURIComponent(name)}&userEmail=${encodeURIComponent(email)}`, {
            method: 'POST'
        });

        if(res.ok){
            document.getElementById('tempPwEmail').innerHTML = `<strong>${email}</strong>로<br>임시 비밀번호를 전송했습니다.<br>로그인 후 비밀번호를 변경해 주세요.`;
            openModal('modalTempPwSent');
        } else {
            openModal('modalFindPwFail');
        }
    } catch(e) {
        openModal('modalFindPwFail');
    }
}

function updatePwStrength(){
    const pw=document.getElementById('newPw')?.value||'';
    const bars=[document.getElementById('pb1'),document.getElementById('pb2'),document.getElementById('pb3')];
    const label=document.getElementById('pwStrengthLabel');
    const strength=pw.length>=8?pw.length>=12?3:2:pw.length>=4?1:0;
    const colors=['var(--light-gray)','var(--red-err)','var(--amber)','var(--green)'];
    const labels=['입력하세요','약함','보통','강함'];
    bars.forEach((b,i)=>{if(b)b.style.background=i<strength?colors[strength]:'var(--light-gray)';});
    if(label)label.textContent=labels[strength];
}

function updatePwStrength2(){
    const pw=document.getElementById('newPw')?.value||'';
    const bars=[document.getElementById('pb21'),document.getElementById('pb22'),document.getElementById('pb23')];
    const label=document.getElementById('pwStr2Label');
    const strength=pw.length>=8?pw.length>=12?3:2:pw.length>=4?1:0;
    const colors=['var(--light-gray)','var(--red-err)','var(--amber)','var(--green)'];
    const labels=['입력하세요','약함','보통','강함'];
    bars.forEach((b,i)=>{if(b)b.style.background=i<strength?colors[strength]:'var(--light-gray)';});
    if(label)label.textContent=labels[strength];
}

function doPwReset(){showToast('비밀번호가 변경되었습니다! 다시 로그인해 주세요.');navigate('login');}

function doLogout() {
    state.loggedIn=false;
    state.user=null;
    localStorage.removeItem('jwt');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userEmail');
    closeModal('modalLogout');
    showToast('로그아웃되었습니다.');
    navigate('main');
}

// ══════════════════════════════════════
// 사이드바
// ══════════════════════════════════════
function requireLogin(page){
    if(!state.loggedIn){
        openModal('modalNeedLogin');
        state._pendingPage=page;
        return true;
    }
    return false;
}

