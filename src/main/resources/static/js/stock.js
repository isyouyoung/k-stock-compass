async function pgStockIndex(){
    document.getElementById('mainContent').innerHTML = `<div class="page-wrap">
        <div class="page-header"><div class="page-title">📊 시장 지수 조회</div><div class="page-sub">KIS API 실시간 기준</div></div>
        <div class="grid2 mb16">
        <div class="card" style="border-top:4px solid var(--navy);" id="kospiCard">
        <div style="font-size:14px;font-weight:700;color:var(--navy);margin-bottom:12px;">KOSPI 코스피</div>
        <div class="skeleton" style="height:48px;margin-bottom:10px;"></div>
        <div class="skeleton" style="height:20px;width:60%;margin-bottom:6px;"></div>
        </div>
        <div class="card" style="border-top:4px solid var(--blue);" id="kosdaqCard">
        <div style="font-size:14px;font-weight:700;color:var(--blue);margin-bottom:12px;">KOSDAQ 코스닥</div>
        <div class="skeleton" style="height:48px;margin-bottom:10px;"></div>
        <div class="skeleton" style="height:20px;width:60%;margin-bottom:6px;"></div>
        </div>
        </div>
        <div class="notice mb16">ℹ 본 시세 정보는 KIS API 실시간 기준입니다.</div>
        <div class="page-wrap" style="padding-top:0;">
        <div class="card">
        <div class="section-title">주요 종목 시세</div>
        <div id="majorStocks"><div style="font-size:13px;color:var(--gray);padding:20px 0;">시세 조회 중...</div></div>
        </div></div>
        </div>`;

    await refreshStockIndex();

    window._stockIndexInterval = setInterval(async () => {
        if(state.currentPage !== 'stock_index') {
            clearInterval(window._stockIndexInterval);
            window._stockIndexInterval = null;
            return;
        }
        await refreshStockIndex();
    }, 1000);
}

async function refreshStockIndex() {
    try {
        const [kospiRes, kosdaqRes] = await Promise.all([
            fetch('/api/stock/index/realtime?indexCode=0001'),
            fetch('/api/stock/index/realtime?indexCode=1001')
        ]);
        const kospi = await kospiRes.json();
        const kosdaq = await kosdaqRes.json();

        const renderCard = (data) => {
            const up = parseFloat(data.vs) >= 0;
            return `
            <div style="font-size:48px;font-weight:900;color:var(--dark);">${Number(data.clpr).toLocaleString()}</div>
            <div class="${up?'up':'down'}" style="font-size:18px;margin:10px 0;">${up?'▲ +':'▼ -'}${Math.abs(parseFloat(data.vs)).toFixed(2)} &nbsp;(${up?'+':''}${parseFloat(data.fltRt).toFixed(2)}%)</div>
            <div style="font-size:12px;color:var(--gray);">전일 대비 &nbsp;|&nbsp; 시가: ${Number(data.mkp).toLocaleString()}</div>
            <div style="font-size:12px;color:var(--gray);margin-top:4px;">고가: ${Number(data.hipr).toLocaleString()} &nbsp;|&nbsp; 저가: ${Number(data.lopr).toLocaleString()}</div>`;
        };

        const kospiEl = document.getElementById('kospiCard');
        const kosdaqEl = document.getElementById('kosdaqCard');
        if(kospiEl) kospiEl.innerHTML = `<div style="font-size:14px;font-weight:700;color:var(--navy);margin-bottom:12px;">KOSPI 코스피</div>${renderCard(kospi)}`;
        if(kosdaqEl) kosdaqEl.innerHTML = `<div style="font-size:14px;font-weight:700;color:var(--blue);margin-bottom:12px;">KOSDAQ 코스닥</div>${renderCard(kosdaq)}`;

    } catch(e) {
        console.error('지수 갱신 실패', e);
    }

    // 주요 종목 5개 갱신
    const majorCodes = [
        {code:'005930', name:'삼성전자', market:'KOSPI'},
        {code:'000660', name:'SK하이닉스', market:'KOSPI'},
        {code:'373220', name:'LG에너지솔루션', market:'KOSPI'},
        {code:'035420', name:'NAVER', market:'KOSPI'},
        {code:'035720', name:'카카오', market:'KOSDAQ'}
    ];

    try {
        const majorResults = await Promise.all(
            majorCodes.map(s => fetch(`/api/stock/detail?stockCode=${s.code}`)
                .then(r=>r.json())
                .then(d=>({...s, ...d}))
                .catch(()=>null))
        );

        const majorEl = document.getElementById('majorStocks');
        if(majorEl) {
            majorEl.innerHTML = majorResults.filter(Boolean).map(d => {
                const price = Number(d.clpr);
                const change = Number(d.vs);
                const pct = Number(d.fltRt);
                return `
                <div class="stock-row" onclick="navigate('stock_detail',{currentStock:'${d.code}'})">
                <span class="sn">${d.name}</span><span class="sc">${d.code}</span>
                <span class="badge badge-${d.market.toLowerCase()}">${d.market}</span>
                <span class="sp ${change>=0?'up':'down'}" style="margin-left:auto;">${fmt(price)}원</span>
                <span class="sch ${change>=0?'up':'down'}">${change>=0?'▲ +':'▼ -'}${fmt(Math.abs(change))} (${pct>=0?'+':''}${pct.toFixed(2)}%)</span>
                </div>`;
            }).join('');
        }
    } catch(e) {
        console.error('주요종목 갱신 실패', e);
    }
}

function pgStockSearch(){
    const stocks=[
        {code:'005930',name:'삼성전자',market:'KOSPI'},
        {code:'000660',name:'SK하이닉스',market:'KOSPI'},
        {code:'373220',name:'LG에너지솔루션',market:'KOSPI'},
        {code:'035420',name:'NAVER',market:'KOSPI'},
        {code:'035720',name:'카카오',market:'KOSDAQ'},
    ];
    window._allStocks = stocks;

    document.getElementById('mainContent').innerHTML = `<div class="page-wrap">
        <div class="page-header"><div class="page-title">🔍 종목 검색</div></div>
        <div class="card mb16">
        <div class="search-bar mb12"><input id="stockSearchInput" placeholder="종목명 또는 종목코드 입력 (예: 삼성전자, 005930)" oninput="doStockSearch()" onkeydown="if(event.key==='Enter')doStockSearch()"><button onclick="doStockSearch()">🔍 검색</button></div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
        ${['삼성전자','SK하이닉스','NAVER','카카오'].map(k=>`<span class="tag" onclick="document.getElementById('stockSearchInput').value='${k}';doStockSearch()">${k}</span>`).join('')}
        </div></div>
        <div id="searchResults">
        <div class="card"><div class="section-title">인기 종목 TOP 5</div>
        <div id="topStockList"><div style="font-size:13px;color:var(--gray);padding:20px 0;">시세 조회 중...</div></div>
        </div></div></div>`;

    const loadTopStocks = () => {
        Promise.all(
            stocks.map(s => fetch(`/api/stock/detail?stockCode=${s.code}`)
                .then(r => r.json())
                .then(d => ({...s, ...d}))
                .catch(() => s))
        ).then(results => {
            const el = document.getElementById('topStockList');
            if(!el) return;
            el.innerHTML = results.map((s, i) => {
                const price = Number(s.clpr || 0);
                const change = Number(s.vs || 0);
                const pct = Number(s.fltRt || 0);
                return `
                <div class="stock-row" onclick="navigate('stock_detail',{currentStock:'${s.code}'})">
                <span style="font-size:16px;font-weight:700;color:${i<3?'var(--blue)':'var(--gray)'};min-width:28px;">${i+1}</span>
                <span class="sn">${s.name}</span><span class="sc">${s.code}</span>
                <span class="badge badge-${s.market.toLowerCase()}">${s.market}</span>
                <span class="sp ${change>=0?'up':'down'}" style="margin-left:auto;">${fmt(price)}원</span>
                <span class="sch ${change>=0?'up':'down'}">${change>=0?'▲ +':'▼ -'}${fmt(Math.abs(change))} (${pct>=0?'+':''}${pct.toFixed(2)}%)</span>
                </div>`;
            }).join('');
        });
    };

    loadTopStocks();

    window._stockSearchInterval = setInterval(() => {
        if(state.currentPage !== 'stock_search') {
            clearInterval(window._stockSearchInterval);
            window._stockSearchInterval = null;
            return;
        }
        loadTopStocks();
    }, 1000);
}

function initSearch(){}

async function doStockSearch(){
    const q = (document.getElementById('stockSearchInput')||{value:''}).value.trim();
    const container = document.getElementById('searchResults');
    if(!container) return;

    // 빈 검색이면 인기종목 표시 (기존 하드코딩 유지)
    if(!q){
        container.innerHTML = `<div class="card"><div class="section-title">인기 종목 TOP 5</div>${(window._allStocks||[]).slice(0,5).map((s,i)=>`
        <div class="stock-row" onclick="navigate('stock_detail',{currentStock:'${s.code}'})">
        <span style="font-size:16px;font-weight:700;color:${i<3?'var(--blue)':'var(--gray)'};min-width:28px;">${i+1}</span>
        <span class="sn">${s.name}</span><span class="sc">${s.code}</span>
        <span class="badge badge-kospi">KOSPI</span>
        </div>`).join('')}</div>`;
        return;
    }

    // 로딩 표시
    container.innerHTML = `<div class="card"><div style="font-size:13px;color:var(--gray);">검색 중...</div></div>`;

    try {
        const res = await fetch(`/api/stock/search?query=${encodeURIComponent(q)}&type=name`);
        const results = await res.json();

        if(!results.length){
            container.innerHTML=`<div class="card"><div style="font-size:13px;color:var(--gray);margin-bottom:20px;">'${q}' 검색 결과 <strong style="color:var(--red-err);">0건</strong></div>
        <div class="empty"><div class="empty-icon">🔍</div><div class="empty-title">검색 결과가 없습니다</div>
        <div class="empty-sub">종목명 또는 6자리 종목코드를 정확히 입력해 주세요</div></div></div>`;
            return;
        }

        const rows = results.map(s => {
            const isFav = state.favorites.some(f => f.code === s.stockCd);
            return `
        <div class="stock-row" onclick="navigate('stock_detail',{currentStock:'${s.stockCd}'})">
        <span class="sn">${s.stockNm}</span>
        <span class="sc">${s.stockCd}</span>
        ${state.loggedIn ? `<button class="btn btn-ghost btn-sm" style="margin-left:auto;color:${isFav ? '#FBBF24' : 'var(--gray)'};" onclick="event.stopPropagation();openFavAdd('${s.stockNm}','${s.stockCd}')">${isFav ? '★' : '☆'}</button>` : ''}

        </div>`;
        }).join('');

        container.innerHTML=`<div class="card"><div style="font-size:13px;color:var(--gray);margin-bottom:16px;">'${q}' 검색 결과 <strong style="color:var(--navy);">${results.length}건</strong></div>${rows}</div>`;

    } catch(e) {
        container.innerHTML=`<div class="card"><div style="color:var(--red-err);font-size:13px;">검색 중 오류가 발생했습니다.</div></div>`;
    }
}

function doMainSearch(){
    const q=document.getElementById('mainSearch').value;
    navigate('stock_search');
    setTimeout(()=>{const el=document.getElementById('stockSearchInput');if(el){el.value=q;doStockSearch();}},50);
}

async function pgStockDetail(){
    // 기존 인터벌 제거
    if(window._stockDetailInterval) {
        clearInterval(window._stockDetailInterval);
        window._stockDetailInterval = null;
    }

    document.getElementById('mainContent').innerHTML = `<div class="page-wrap"><div class="card"><div style="padding:40px;text-align:center;color:var(--gray);">로딩 중...</div></div></div>`;

    await loadStockDetail(state.currentStock);

    // 10초마다 가격 갱신 => 3초로 바꿈 => 혼자 쓸때는 1초 가능
    window._stockDetailInterval = setInterval(async () => {
        if(state.currentPage !== 'stock_detail') {
            clearInterval(window._stockDetailInterval);
            window._stockDetailInterval = null;
            return;
        }
        await refreshStockPrice(state.currentStock);
    }, 1000);
}

async function loadStockDetail(stockCode) {
    try {
        const [stockRes, finRes] = await Promise.all([
            fetch(`/api/stock/detail?stockCode=${stockCode}`),
            fetch(`/api/financial/${stockCode}`)
        ]);
        const data = await stockRes.json();
        const fin = finRes.ok ? await finRes.json() : null;

        const s = {
            code: data.srtnCd,
            name: data.itmsNm,
            market: data.mrktCtg || 'KOSPI',
            price: Number(data.clpr),
            change: Number(data.vs),
            pct: Number(data.fltRt),
            oprc: data.oprc,
            hgpr: data.hgpr,
            lwpr: data.lwpr,
            acmlVol: data.acmlVol,
            htsMktcap: data.htsMktcap,
            w52Hgpr: data.w52Hgpr
        };

        const isFav = state.favorites.some(f => f.code === s.code);
        const tab = state.detailTab || 'info';

        // AI 탭일 때만 Gemini 호출
        let ai = null;
        if (tab === 'ai') {
            const aiRes = await fetch(`/api/ai/signal/${stockCode}?stockName=${encodeURIComponent(s.name)}`);
            if (aiRes.ok) ai = await aiRes.json();
        }

        document.getElementById('mainContent').innerHTML = `
        <div class="detail-header">
        <div><div class="detail-name">${s.name}</div><div class="detail-meta">${s.code} &nbsp;|&nbsp; <span class="badge badge-${s.market.toLowerCase()}">${s.market}</span></div></div>
        <div class="detail-price">
        <div class="price" id="stockPrice">${fmt(s.price)}원</div>
        <div class="change ${s.change>=0?'up':'down'}" id="stockChange">${s.change>=0?'▲ +':'▼ -'}${fmt(Math.abs(s.change))} &nbsp;(${s.pct>=0?'+':''}${s.pct.toFixed(2)}%)</div>
        </div></div>
        <div class="inner-tabs">
        <div class="inner-tab ${tab==='info'?'active':''}" onclick="navigate('stock_detail',{currentStock:'${s.code}',detailTab:'info'})">종목 정보</div>
        <div class="inner-tab ${tab==='ai'?'active':''}" onclick="navigate('stock_detail',{currentStock:'${s.code}',detailTab:'ai'})">AI 신호등</div>
        </div>
        <div class="page-wrap">
        ${tab==='info'?detailInfo(s,isFav,fin):detailAI(s,ai)}
        </div>`;

    } catch(e) {
        showToast('종목 정보 조회에 실패했습니다.');
    }
}

async function refreshStockPrice(stockCode) {
    try {
        const res = await fetch(`/api/stock/detail?stockCode=${stockCode}`);
        const data = await res.json();

        const priceEl = document.getElementById('stockPrice');
        const changeEl = document.getElementById('stockChange');
        if(!priceEl || !changeEl) return;

        const price = Number(data.clpr);
        const change = Number(data.vs);
        const pct = Number(data.fltRt);

        priceEl.textContent = fmt(price) + '원';
        changeEl.className = `change ${change>=0?'up':'down'}`;
        changeEl.innerHTML = `${change>=0?'▲ +':'▼ -'}${fmt(Math.abs(change))} &nbsp;(${pct>=0?'+':''}${pct.toFixed(2)}%)`;
    } catch(e) {
        // 조용히 실패
    }
}

function detailInfo(s,isFav,fin){return `
        <div class="grid3 mb16">
        ${[
    ['현재가', `${fmt(s.price)}원`],
    ['전일 대비', `${s.change >= 0 ? '▲ +' : '▼ -'}${fmt(Math.abs(s.change))}`],
    ['등락률', `${s.pct >= 0 ? '+' : ''}${s.pct.toFixed(2)}%`],
    ['시가', `${fmt(Number(s.oprc))}원`],
    ['고가', `${fmt(Number(s.hgpr))}원`],
    ['저가', `${fmt(Number(s.lwpr))}원`],
    ['거래량', `${fmt(Number(s.acmlVol))}주`],
    ['시가총액', formatMktcap(s.htsMktcap)],
    ['52주 최고', `${fmt(Number(s.w52Hgpr))}원`]
].map(([l, v]) => `
    <div class="card"><div style="font-size:11px;color:var(--gray);margin-bottom:6px;">${l}</div><div style="font-size:16px;font-weight:700;">${v}</div></div>`).join('')}
        </div>
        <div style="font-size:11px;color:var(--gray);margin-bottom:12px;">※ 거래량은 정규장 매수 체결 기준이며 실제와 다소 차이가 있을 수 있습니다.</div>
        <div class="card mb16">
        <div class="flex flex-between flex-center mb12">
        <div><div class="section-title" style="margin-bottom:2px;">기업 재무 정보</div><div style="font-size:12px;color:var(--gray);">DART 전자공시 API · 최근 분기 기준</div></div>
        </div>
        <div class="grid3">
        ${(fin ? [
            ['부채비율', fin.debtRatio === -1 ? '자본잠식' : fin.debtRatio !== null ? fin.debtRatio + '%' : 'N/A', fin.bsnsYear + '년 기준', fin.debtRatio !== null && fin.debtRatio !== -1 && fin.debtRatio < 100 ? 'var(--green)' : 'var(--red-err)'],
            ['영업이익률', fin.operatingMargin !== null ? fin.operatingMargin + '%' : 'N/A', fin.bsnsYear + '년 기준', fin.operatingMargin !== null && fin.operatingMargin > 0 ? 'var(--green)' : 'var(--red-err)'],
            ['유동비율', fin.currentRatio !== null ? fin.currentRatio + '%' : 'N/A', fin.bsnsYear + '년 기준', fin.currentRatio !== null && fin.currentRatio > 100 ? 'var(--green)' : 'var(--red-err)'],
            ['매출액', formatFinAmt(fin.revenue), fin.bsnsYear + '년 기준', 'var(--dark)'],
            ['영업이익', formatFinAmt(fin.operatingProfit), fin.bsnsYear + '년 기준', 'var(--dark)'],
            ['당기순이익', formatFinAmt(fin.netIncome), fin.bsnsYear + '년 기준', 'var(--dark)']
        ] : [
            ['부채비율','N/A','데이터 없음','var(--gray)'],
            ['영업이익률','N/A','데이터 없음','var(--gray)'],
            ['유동비율','N/A','데이터 없음','var(--gray)'],
            ['매출액','N/A','데이터 없음','var(--gray)'],
            ['영업이익','N/A','데이터 없음','var(--gray)'],
            ['당기순이익','N/A','데이터 없음','var(--gray)']
        ]).map(([l,v,s2,c])=>`
        <div class="fin-card"><div class="fin-bar" style="background:${c};"></div><div class="fin-label">${l}</div><div class="fin-val" style="color:${c};">${v}</div><div class="fin-sub">${s2}</div></div>`).join('')}
        </div>
        </div>
        <div class="flex gap12">
        ${state.loggedIn ? `
        <button class="btn ${isFav ? 'btn-secondary' : 'btn-outline'} btn-full" onclick="openFavAdd('${s.name}','${s.code}')">${isFav ? '✅ 관심종목 추가됨' : '⭐ 관심종목 추가'}</button>
        <button class="btn btn-primary btn-full" onclick="state._alertFromDetail=true;navigate('alert_add',{alertStock:{code:'${s.code}',name:'${s.name}',price:${s.price}}})">🔔 알림 설정</button>`
    : `<div class="notice btn-full" style="text-align:center;">로그인 후 관심종목 추가 및 알림 설정이 가능합니다. <span style="color:var(--blue);cursor:pointer;font-weight:600;" onclick="navigate('login')">로그인 →</span></div>`}
        </div>`;}

function detailAI(s, ai) {
    if(!state._aiMessages) state._aiMessages={};
    if(!state._aiMessages[s.code]) state._aiMessages[s.code]=[
        {role:'ai',text:`안녕하세요! ${s.name} 재무 분석 AI입니다. 궁금한 점을 질문해 주세요 😊`}
    ];
    const msgs=state._aiMessages[s.code];

    const score = ai ? ai.score : 0;
    const summary = ai ? ai.summary : '로딩 중...';

    const signals = [
        ['#DC2626','1점','매우위험'],
        ['#F97316','2점','위험'],
        ['#EAB308','3점','보통'],
        ['#22C55E','4점','양호'],
        ['#16A34A','5점','매우안전']
    ];
    const scoreLabels = ['','매우위험','위험','보통','양호','매우안전'];

    return `
        <div class="card mb16">
        <div class="flex flex-between flex-center mb12">
        <div><div class="section-title" style="margin-bottom:2px;">AI 재무 신호등</div>
        <div style="font-size:12px;color:var(--gray);">Gemini AI 기반 재무 안정성 분석</div></div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px;align-items:center;">
        <div>
        <div class="signal-wrap mb12">
        ${signals.map(([c,sc,lb],i)=>{
        const act = (i+1) === score;
        return `<div><div class="signal-dot ${act?'big':'small'}" style="background:${act?c:'#D1D5DB'};">${sc}</div>
            <div class="signal-label" style="color:${act?'var(--dark)':'var(--gray)'};font-weight:${act?'700':'400'};">${lb}</div></div>`;
    }).join('')}
        </div>
        ${score > 0 ? `<div class="success-bar" style="font-size:13px;font-weight:600;">종합 평가: ${scoreLabels[score]} (${score}점/5점)</div>` : ''}
        </div>
        <div style="background:var(--bg);border-radius:10px;padding:20px;height:100%;box-sizing:border-box;">
        <div style="font-size:13px;font-weight:700;color:var(--navy);margin-bottom:10px;">📋 재무 분석 요약</div>
        <div style="font-size:13px;color:var(--dark);line-height:1.9;">${summary}</div>
        <div style="font-size:11px;color:var(--gray);margin-top:12px;">분석 기준: DART 공시 기반 · Gemini AI 분석</div>
        </div></div></div>
        <div class="card" style="display:flex;flex-direction:column;height:420px;">
        <div class="section-title mb12">💬 AI 에이전트에게 질문하기</div>
        <div id="aiChatBox" style="flex:1;overflow-y:auto;padding:8px 0;display:flex;flex-direction:column;gap:10px;">
        ${msgs.map(m=>`
        <div style="display:flex;justify-content:${m.role==='user'?'flex-end':'flex-start'};">
        <div style="max-width:75%;padding:10px 14px;border-radius:${m.role==='user'?'12px 12px 2px 12px':'12px 12px 12px 2px'};background:${m.role==='user'?'var(--navy)':'var(--bg)'};color:${m.role==='user'?'white':'var(--dark)'};font-size:13px;line-height:1.6;">
        ${m.text}
        </div></div>`).join('')}
        </div>
        <div style="border-top:1px solid var(--border);padding-top:12px;display:flex;gap:8px;margin-top:8px;">
        <input class="input" id="aiInput_${s.code}" placeholder="${s.name}에 대해 궁금한 점을 입력하세요..." style="flex:1;" onkeydown="if(event.key==='Enter')sendAiMsg('${s.code}','${s.name}')">
        <button class="btn btn-primary" onclick="sendAiMsg('${s.code}','${s.name}')">전송</button>
        </div>
        <div style="font-size:11px;color:var(--gray);margin-top:6px;">분석은 Gemini AI 기반이며 투자 조언이 아닙니다.</div>
        </div>`;
}

async function sendAiMsg(code, name){
    const input=document.getElementById(`aiInput_${code}`);
    if(!input) return;
    const text=input.value.trim();
    if(!text) return;
    if(!state._aiMessages) state._aiMessages={};
    if(!state._aiMessages[code]) state._aiMessages[code]=[];
    state._aiMessages[code].push({role:'user',text});
    input.value='';

    // 채팅창에 사용자 메시지 + 로딩 메시지 직접 추가
    const chatBox = document.getElementById('aiChatBox');
    if(chatBox) {
        const userDiv = document.createElement('div');
        userDiv.style.cssText = 'display:flex;justify-content:flex-end;';
        userDiv.innerHTML = `<div style="max-width:75%;padding:10px 14px;border-radius:12px 12px 2px 12px;background:var(--navy);color:white;font-size:13px;line-height:1.6;">${text}</div>`;
        chatBox.appendChild(userDiv);

        const loadingDiv = document.createElement('div');
        loadingDiv.id = 'aiLoadingMsg';
        loadingDiv.style.cssText = 'display:flex;justify-content:flex-start;';
        loadingDiv.innerHTML = `<div style="max-width:75%;padding:10px 14px;border-radius:12px 12px 12px 2px;background:var(--bg);color:var(--dark);font-size:13px;line-height:1.6;">분석 중... ⏳</div>`;
        chatBox.appendChild(loadingDiv);
        chatBox.scrollTop = chatBox.scrollHeight;
    }

    try {
        const token = localStorage.getItem('jwt');
        const res = await fetch(`/api/ai/chat/${code}?stockName=${encodeURIComponent(name)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? {'Authorization': 'Bearer ' + token} : {})
            },
            body: JSON.stringify({ message: text })
        });

        // 로딩 메시지 제거
        const loadingEl = document.getElementById('aiLoadingMsg');
        if(loadingEl) loadingEl.remove();

        let replyText = '죄송합니다. 답변을 생성하는 중 오류가 발생했습니다.';
        if (res.ok) {
            const data = await res.json();
            replyText = data.reply;
        }

        state._aiMessages[code].push({role:'ai', text: replyText});

        // 채팅창에 AI 답변 직접 추가
        const chatBox2 = document.getElementById('aiChatBox');
        if(chatBox2) {
            const aiDiv = document.createElement('div');
            aiDiv.style.cssText = 'display:flex;justify-content:flex-start;';
            aiDiv.innerHTML = `<div style="max-width:75%;padding:10px 14px;border-radius:12px 12px 12px 2px;background:var(--bg);color:var(--dark);font-size:13px;line-height:1.6;">${replyText}</div>`;
            chatBox2.appendChild(aiDiv);
            chatBox2.scrollTop = chatBox2.scrollHeight;
        }

    } catch(e) {
        const loadingEl = document.getElementById('aiLoadingMsg');
        if(loadingEl) loadingEl.remove();
        state._aiMessages[code].push({role:'ai', text:'네트워크 오류가 발생했습니다.'});

        const chatBox2 = document.getElementById('aiChatBox');
        if(chatBox2) {
            const aiDiv = document.createElement('div');
            aiDiv.style.cssText = 'display:flex;justify-content:flex-start;';
            aiDiv.innerHTML = `<div style="max-width:75%;padding:10px 14px;border-radius:12px 12px 12px 2px;background:var(--bg);color:var(--dark);font-size:13px;line-height:1.6;">네트워크 오류가 발생했습니다.</div>`;
            chatBox2.appendChild(aiDiv);
            chatBox2.scrollTop = chatBox2.scrollHeight;
        }
    }
}

function formatMktcap(val) {
    if (!val) return '-';
    const n = Number(val);  // 이미 억 단위
    const jo = Math.floor(n / 10000);
    const eok = n % 10000;
    if (jo > 0) return `${fmt(jo)}조 ${fmt(eok)}억`;
    return `${fmt(n)}억`;
}

async function pgPortfolio() {
    document.getElementById('mainContent').innerHTML = `<div class="page-wrap">
        <div class="flex flex-between flex-center mb24">
        <div class="page-title">💼 내 포트폴리오</div>
        <button class="btn btn-primary btn-sm" onclick="openPortfolioAdd()">+ 종목 추가</button>
        </div>
        <div id="portfolioContent"><div class="card"><div style="padding:40px;text-align:center;color:var(--gray);">로딩 중...</div></div></div>
        </div>`;

    await loadPortfolio();
}

async function loadPortfolio() {
    const token = localStorage.getItem('jwt');
    try {
        const [portRes, accRes, simRes] = await Promise.all([
            fetch('/api/portfolio', { headers: { 'Authorization': 'Bearer ' + token } }),
            fetch('/api/portfolio/account', { headers: { 'Authorization': 'Bearer ' + token } }),
            fetch('/api/simulator', { headers: { 'Authorization': 'Bearer ' + token } })
        ]);
        const portfolio = await portRes.json();
        const account = await accRes.json();
        const simData = await simRes.json();

        const totalEval = portfolio.reduce((sum, p) => sum + Number(p.evalAmt), 0);
        const totalInvest = portfolio.reduce((sum, p) => sum + Number(p.avgPrice) * Number(p.quantity), 0);
        const totalProfit = totalEval - totalInvest;
        const totalProfitRate = totalInvest > 0 ? (totalProfit / totalInvest * 100) : 0;
        const cash = Number(account.cash || 0);
        const loan = Number(account.loan || 0);
        const netAsset = totalEval + cash - loan;

        const el = document.getElementById('portfolioContent');
        if (!el) return;

        el.innerHTML = `
        <!-- 요약 카드 -->
        <div class="grid3 mb16">
        <div class="card" style="text-align:center;">
        <div style="font-size:11px;color:var(--gray);margin-bottom:6px;">총 평가금액</div>
        <div style="font-size:18px;font-weight:700;">${fmt(totalEval)}원</div>
        </div>
        <div class="card" style="text-align:center;">
        <div style="font-size:11px;color:var(--gray);margin-bottom:6px;">총 손익</div>
        <div style="font-size:18px;font-weight:700;" class="${totalProfit>=0?'up':'down'}">${totalProfit>=0?'+':''}${fmt(Math.round(totalProfit))}원</div>
        <div style="font-size:12px;" class="${totalProfit>=0?'up':'down'}">(${totalProfitRate>=0?'+':''}${totalProfitRate.toFixed(2)}%)</div>
        </div>
        <div class="card" style="text-align:center;">
        <div style="font-size:11px;color:var(--gray);margin-bottom:6px;">순자산</div>
        <div style="font-size:18px;font-weight:700;">${fmt(Math.round(netAsset))}원</div>
        </div>
        </div>

        <!-- 예수금/대출금 -->
        <div class="card mb16">
        <div class="flex flex-between flex-center mb12">
        <div class="section-title" style="margin-bottom:0;">💰 계좌 정보</div>
        <button class="btn btn-ghost btn-sm" onclick="openAccountEdit(${cash}, ${loan})">수정</button>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
        <div><div style="font-size:12px;color:var(--gray);margin-bottom:4px;">예수금</div>
        <div style="font-size:16px;font-weight:700;color:var(--green);">${fmt(cash)}원</div></div>
        <div><div style="font-size:12px;color:var(--gray);margin-bottom:4px;">대출금</div>
        <div style="font-size:16px;font-weight:700;color:var(--red-err);">${fmt(loan)}원</div></div>
        </div>
        </div>

        <!-- 보유종목 -->
        <div class="card mb16">
        <div class="section-title mb12">📋 보유종목</div>
        ${portfolio.length === 0 ? `
        <div class="empty"><div class="empty-icon">💼</div>
        <div class="empty-title">보유 종목이 없습니다</div>
        <div class="empty-sub">종목을 추가해보세요</div></div>` :
            portfolio.map(p => {
                const profit = Number(p.profitAmt);
                const rate = Number(p.profitRate);
                return `
            <div style="padding:14px 0;border-bottom:1px solid var(--border);">
            <div class="flex flex-between flex-center mb8">
            <div>
            <span style="font-size:15px;font-weight:600;">${p.stockNm}</span>
            <span style="font-size:12px;color:var(--gray);margin-left:8px;">${p.stockCd}</span>
            </div>
            <button class="btn btn-sm" style="background:#FFF5F5;color:var(--red-err);border:1px solid var(--red-err);" onclick="deletePortfolio(${p.portId})">삭제</button>
            </div>
            <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:8px;font-size:12px;">
            <div><div style="color:var(--gray);margin-bottom:2px;">평균매수가</div><div style="font-weight:600;">${fmt(p.avgPrice)}원</div></div>
            <div><div style="color:var(--gray);margin-bottom:2px;">현재가</div><div style="font-weight:600;" id="port-price-${p.stockCd}">${fmt(p.currentPrice)}원</div></div>
            <div><div style="color:var(--gray);margin-bottom:2px;">수량</div><div style="font-weight:600;">${fmt(p.quantity)}주</div></div>
            <div><div style="color:var(--gray);margin-bottom:2px;">평가금</div><div style="font-weight:600;" id="port-eval-${p.stockCd}">${fmt(Math.round(Number(p.evalAmt)))}원</div></div>
            <div><div style="color:var(--gray);margin-bottom:2px;">손익</div>
            <div style="font-weight:600;" id="port-profit-${p.stockCd}" class="${profit>=0?'up':'down'}">${profit>=0?'+':''}${fmt(Math.round(profit))}원</div>
            <div style="font-size:11px;" id="port-rate-${p.stockCd}" class="${profit>=0?'up':'down'}">(${rate>=0?'+':''}${rate.toFixed(2)}%)</div>
            </div>
            </div>
            </div>`;
            }).join('')}
        </div>
        
        <!-- 수익 시뮬레이터 -->
        <div class="card">
        <div class="flex flex-between flex-center mb12">
        <div class="section-title" style="margin-bottom:0;">🧮 수익 시뮬레이터</div>
        <button class="btn btn-primary btn-sm" onclick="openSimulatorAdd()">+ 종목 추가</button>
        </div>
        <div id="simList">
        ${simData.length === 0 ? `
        <div class="empty"><div class="empty-icon">🧮</div>
        <div class="empty-title">시뮬레이션 종목이 없습니다</div>
        <div class="empty-sub">종목을 추가해서 수익을 예상해보세요</div></div>` :
            simData.map(s => {
                const profit = Number(s.expectedProfit);
                const rate = Number(s.expectedProfitRate);
                return `
            <div style="padding:14px;border:1px solid var(--border);border-radius:10px;margin-bottom:10px;">
            <div class="flex flex-between flex-center mb10">
            <div>
            <span style="font-size:15px;font-weight:600;">${s.stockNm}</span>
            <span style="font-size:12px;color:var(--gray);margin-left:8px;">${s.stockCd}</span>
            </div>
            <div style="display:flex;gap:6px;">
            <button class="btn btn-sm btn-outline" onclick="openSimulatorEdit(${s.simId},${s.avgPrice},${s.quantity},${s.targetPrice})">수정</button>
            <button class="btn btn-sm" style="background:#FFF5F5;color:var(--red-err);border:1px solid var(--red-err);" onclick="deleteSimulator(${s.simId})">삭제</button>
            </div>
            </div>
            <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:8px;font-size:12px;">
            <div><div style="color:var(--gray);margin-bottom:2px;">평단가</div><div style="font-weight:600;">${fmt(s.avgPrice)}원</div></div>
            <div><div style="color:var(--gray);margin-bottom:2px;">수량</div><div style="font-weight:600;">${fmt(s.quantity)}주</div></div>
            <div><div style="color:var(--gray);margin-bottom:2px;">목표가</div><div style="font-weight:600;">${fmt(s.targetPrice)}원</div></div>
            <div><div style="color:var(--gray);margin-bottom:2px;">예상매도금</div><div style="font-weight:600;">${fmt(s.expectedRevenue)}원</div></div>
            <div><div style="color:var(--gray);margin-bottom:2px;">예상손익</div>
            <div style="font-weight:600;" class="${profit>=0?'up':'down'}">${profit>=0?'+':''}${fmt(Math.round(profit))}원</div>
            <div style="font-size:11px;" class="${profit>=0?'up':'down'}">(${rate>=0?'+':''}${rate.toFixed(2)}%)</div>
            </div>
            </div>
            </div>`;
            }).join('')}
        </div>
        <!-- 예상 총 잔액 -->
        ${simData.length > 0 ? `
        <div style="margin-top:16px;background:var(--navy);border-radius:10px;padding:20px;color:white;">
        <div style="font-size:13px;opacity:0.8;margin-bottom:12px;">💰 예상 총 잔액 계산</div>
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;text-align:center;margin-bottom:16px;">
        <div><div style="font-size:11px;opacity:0.7;margin-bottom:4px;">예상 매도 합계</div><div style="font-weight:700;">${fmt(Math.round(simData.reduce((s,x)=>s+Number(x.expectedRevenue),0)))}원</div></div>
        <div><div style="font-size:11px;opacity:0.7;margin-bottom:4px;">예수금</div><div style="font-weight:700;color:#86EFAC;">${fmt(cash)}원</div></div>
        <div><div style="font-size:11px;opacity:0.7;margin-bottom:4px;">대출금</div><div style="font-weight:700;color:#FCA5A5;">${fmt(loan)}원</div></div>
        <div><div style="font-size:11px;opacity:0.7;margin-bottom:4px;">예상 총 잔액</div>
        <div style="font-size:18px;font-weight:900;color:#FDE047;">${fmt(Math.round(simData.reduce((s,x)=>s+Number(x.expectedRevenue),0) + cash - loan))}원</div>
        </div>
        </div>
        </div>` : ''}
        </div>
        </div>`;

        // 보유종목 실시간 가격 갱신
        if(portfolio.length > 0) {
            if(window._portfolioInterval) clearInterval(window._portfolioInterval);
            window._portfolioInterval = setInterval(async () => {
                if(state.currentPage !== 'portfolio') {
                    clearInterval(window._portfolioInterval);
                    window._portfolioInterval = null;
                    return;
                }
                await Promise.all(portfolio.slice(0, 8).map(async p => {
                    try {
                        const res = await fetch(`/api/stock/detail?stockCode=${p.stockCd}`);
                        const d = await res.json();
                        const currentPrice = Number(d.clpr);
                        const evalAmt = currentPrice * Number(p.quantity);
                        const investAmt = Number(p.avgPrice) * Number(p.quantity);
                        const profitAmt = evalAmt - investAmt;
                        const profitRate = investAmt > 0 ? (profitAmt / investAmt * 100) : 0;

                        const priceEl = document.getElementById(`port-price-${p.stockCd}`);
                        const evalEl = document.getElementById(`port-eval-${p.stockCd}`);
                        const profitEl = document.getElementById(`port-profit-${p.stockCd}`);
                        const rateEl = document.getElementById(`port-rate-${p.stockCd}`);

                        if(priceEl) priceEl.textContent = fmt(currentPrice) + '원';
                        if(evalEl) evalEl.textContent = fmt(Math.round(evalAmt)) + '원';
                        if(profitEl) {
                            profitEl.className = profitAmt >= 0 ? 'up' : 'down';
                            profitEl.textContent = `${profitAmt>=0?'+':''}${fmt(Math.round(profitAmt))}원`;
                        }
                        if(rateEl) {
                            rateEl.className = profitRate >= 0 ? 'up' : 'down';
                            rateEl.textContent = `(${profitRate>=0?'+':''}${profitRate.toFixed(2)}%)`;
                        }
                    } catch(e) {}
                }));
            }, 1000);
        }

        // 10초마다 총 자산 저장 (차트 그리기용)
        if(window._assetSaveInterval) clearInterval(window._assetSaveInterval);
        window._assetSaveInterval = setInterval(async () => {
            if(state.currentPage !== 'portfolio') {
                clearInterval(window._assetSaveInterval);
                window._assetSaveInterval = null;
                return;
            }
            try {
                const token = localStorage.getItem('jwt');
                const userEmail = state.user?.email;
                if(!userEmail) return;

                // 현재 총 자산 계산 (평가금 + 예수금 - 대출금)
                const currentTotalEval = portfolio.reduce((sum, p) => {
                    const priceEl = document.getElementById(`port-price-${p.stockCd}`);
                    const currentPrice = priceEl ? Number(priceEl.textContent.replace(/[^0-9]/g, '')) : Number(p.currentPrice);
                    return sum + currentPrice * Number(p.quantity);
                }, 0);
                const totalAsset = Math.round(currentTotalEval + cash - loan);

                await fetch('/api/asset-history', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify({ userEmail, totalAsset })
                });
            } catch(e) {
                console.error('자산 저장 실패', e);
            }
        }, 3000); // 차트 몇초마다 그릴건지

    } catch(e) {
        console.error('포트폴리오 로드 실패', e);
        showToast('포트폴리오 조회에 실패했습니다.');
    }
}

function openPortfolioAdd() {
    const html = `<div class="card" style="max-width:480px;margin:0 auto;">
    <div class="section-title mb16">📌 종목 추가</div>
    <div class="form-group"><div class="label">종목 검색</div>
    <div class="search-bar"><input id="portStockInput" placeholder="종목명 입력" oninput="searchPortStock()"><button onclick="searchPortStock()">🔍</button></div>
    <div id="portStockResults"></div>
    <input type="hidden" id="portStockCd">
    </div>
    <div class="form-group"><div class="label">평균매수가 (원)</div>
    <input class="input" id="portAvgPrice" type="number" placeholder="평균 매수가 입력"></div>
    <div class="form-group"><div class="label">보유 수량 (주)</div>
    <input class="input" id="portQty" type="number" placeholder="보유 수량 입력"></div>
    <div style="display:flex;gap:8px;">
    <button class="btn btn-primary btn-full" onclick="doAddPortfolio()">추가</button>
    <button class="btn btn-secondary btn-full" onclick="loadPortfolio()">취소</button>
    </div></div>`;
    document.getElementById('portfolioContent').innerHTML = html;
}

async function searchPortStock() {
    const q = (document.getElementById('portStockInput')?.value || '').trim();
    const container = document.getElementById('portStockResults');
    if (!q || !container) return;
    try {
        const res = await fetch(`/api/stock/search?query=${encodeURIComponent(q)}&type=name`);
        const results = await res.json();
        if (!results.length) { container.innerHTML = ''; return; }
        container.innerHTML = `<div class="card" style="padding:8px 0;margin-top:4px;">${results.slice(0,5).map(s=>`
            <div style="padding:10px 16px;cursor:pointer;font-size:13px;display:flex;gap:10px;" onclick="selectPortStock('${s.stockCd}','${s.stockNm}')">
            <span style="font-weight:600;">${s.stockNm}</span><span style="color:var(--gray);font-size:11px;">${s.stockCd}</span>
            </div>`).join('')}</div>`;
    } catch(e) { container.innerHTML = ''; }
}

function selectPortStock(code, name) {
    document.getElementById('portStockInput').value = name;
    document.getElementById('portStockCd').value = code;
    document.getElementById('portStockResults').innerHTML = '';
}

async function doAddPortfolio() {
    const code = document.getElementById('portStockCd').value;
    const avgPrice = document.getElementById('portAvgPrice').value;
    const qty = document.getElementById('portQty').value;
    const token = localStorage.getItem('jwt');

    if (!code || !avgPrice || !qty) { showToast('모든 항목을 입력해 주세요.'); return; }

    try {
        const res = await fetch('/api/portfolio', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
            body: JSON.stringify({ stockCd: code, avgPrice: parseInt(avgPrice), quantity: parseInt(qty) })
        });
        if (res.ok) {
            showToast('종목이 추가되었습니다! 💼');
            await loadPortfolio();
        } else {
            showToast('추가에 실패했습니다.');
        }
    } catch(e) { showToast('서버 오류가 발생했습니다.'); }
}

async function deletePortfolio(portId) {
    const token = localStorage.getItem('jwt');
    try {
        const res = await fetch(`/api/portfolio/${portId}`, {
            method: 'DELETE',
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (res.ok) {
            showToast('삭제되었습니다.');
            await loadPortfolio();
        }
    } catch(e) { showToast('서버 오류가 발생했습니다.'); }
}

function openAccountEdit(cash, loan) {
    const html = `<div class="card" style="max-width:480px;margin:0 auto;">
    <div class="section-title mb16">💰 계좌 정보 수정</div>
    <div class="form-group"><div class="label">예수금 (원)</div>
    <input class="input" id="editCash" type="number" value="${cash}" placeholder="예수금 입력"></div>
    <div class="form-group"><div class="label">대출금 (원)</div>
    <input class="input" id="editLoan" type="number" value="${loan}" placeholder="대출금 입력"></div>
    <div style="display:flex;gap:8px;">
    <button class="btn btn-primary btn-full" onclick="doUpdateAccount()">저장</button>
    <button class="btn btn-secondary btn-full" onclick="loadPortfolio()">취소</button>
    </div></div>`;
    document.getElementById('portfolioContent').innerHTML = html;
}

async function doUpdateAccount() {
    const cash = document.getElementById('editCash').value;
    const loan = document.getElementById('editLoan').value;
    const token = localStorage.getItem('jwt');
    try {
        const res = await fetch('/api/portfolio/account', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
            body: JSON.stringify({ cash: parseInt(cash||0), loan: parseInt(loan||0) })
        });
        if (res.ok) {
            showToast('계좌 정보가 저장되었습니다!');
            await loadPortfolio();
        }
    } catch(e) { showToast('서버 오류가 발생했습니다.'); }
}

function openSimulatorAdd() {
    const html = `<div class="card" style="max-width:480px;margin:0 auto;">
    <div class="section-title mb16">🧮 시뮬레이터 종목 추가</div>
    <div class="form-group"><div class="label">종목 검색</div>
    <div class="search-bar"><input id="simStockInput" placeholder="종목명 입력" oninput="searchSimStock()"><button onclick="searchSimStock()">🔍</button></div>
    <div id="simStockResults"></div>
    <input type="hidden" id="simStockCd">
    <input type="hidden" id="simStockNm">
    </div>
    <div class="form-group"><div class="label">평단가 (원)</div>
    <input class="input" id="simAvgPrice" type="number" placeholder="평균 매수가 입력"></div>
    <div class="form-group"><div class="label">수량 (주)</div>
    <input class="input" id="simQtyInput" type="number" placeholder="보유 수량 입력"></div>
    <div class="form-group"><div class="label">목표 매도가 (원)</div>
    <input class="input" id="simTargetPrice" type="number" placeholder="목표 매도가 입력"></div>
    <div style="display:flex;gap:8px;">
    <button class="btn btn-primary btn-full" onclick="doAddSimulator()">추가</button>
    <button class="btn btn-secondary btn-full" onclick="loadPortfolio()">취소</button>
    </div></div>`;
    document.getElementById('portfolioContent').innerHTML = html;
}

async function searchSimStock() {
    const q = (document.getElementById('simStockInput')?.value || '').trim();
    const container = document.getElementById('simStockResults');
    if (!q || !container) return;
    try {
        const res = await fetch(`/api/stock/search?query=${encodeURIComponent(q)}&type=name`);
        const results = await res.json();
        if (!results.length) { container.innerHTML = ''; return; }
        container.innerHTML = `<div class="card" style="padding:8px 0;margin-top:4px;">${results.slice(0,5).map(s=>`
            <div style="padding:10px 16px;cursor:pointer;font-size:13px;display:flex;gap:10px;" onclick="selectSimStock('${s.stockCd}','${s.stockNm}')">
            <span style="font-weight:600;">${s.stockNm}</span><span style="color:var(--gray);font-size:11px;">${s.stockCd}</span>
            </div>`).join('')}</div>`;
    } catch(e) { container.innerHTML = ''; }
}

function selectSimStock(code, name) {
    document.getElementById('simStockInput').value = name;
    document.getElementById('simStockCd').value = code;
    document.getElementById('simStockNm').value = name;
    document.getElementById('simStockResults').innerHTML = '';
}

async function doAddSimulator() {
    const code = document.getElementById('simStockCd').value;
    const name = document.getElementById('simStockNm').value;
    const avgPrice = document.getElementById('simAvgPrice').value;
    const qty = document.getElementById('simQtyInput').value;
    const targetPrice = document.getElementById('simTargetPrice').value;
    const token = localStorage.getItem('jwt');

    if (!code || !avgPrice || !qty || !targetPrice) { showToast('모든 항목을 입력해 주세요.'); return; }

    try {
        const res = await fetch('/api/simulator', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
            body: JSON.stringify({ stockCd: code, stockNm: name, avgPrice: parseInt(avgPrice), quantity: parseInt(qty), targetPrice: parseInt(targetPrice) })
        });
        if (res.ok) {
            showToast('시뮬레이터에 추가되었습니다! 🧮');
            await loadPortfolio();
        } else { showToast('추가에 실패했습니다.'); }
    } catch(e) { showToast('서버 오류가 발생했습니다.'); }
}

function openSimulatorEdit(simId, avgPrice, quantity, targetPrice) {
    const html = `<div class="card" style="max-width:480px;margin:0 auto;">
    <div class="section-title mb16">🧮 시뮬레이터 수정</div>
    <div class="form-group"><div class="label">평단가 (원)</div>
    <input class="input" id="editSimAvg" type="number" value="${avgPrice}"></div>
    <div class="form-group"><div class="label">수량 (주)</div>
    <input class="input" id="editSimQty" type="number" value="${quantity}"></div>
    <div class="form-group"><div class="label">목표 매도가 (원)</div>
    <input class="input" id="editSimTarget" type="number" value="${targetPrice}"></div>
    <div style="display:flex;gap:8px;">
    <button class="btn btn-primary btn-full" onclick="doUpdateSimulator(${simId})">수정 완료</button>
    <button class="btn btn-secondary btn-full" onclick="loadPortfolio()">취소</button>
    </div></div>`;
    document.getElementById('portfolioContent').innerHTML = html;
}

async function doUpdateSimulator(simId) {
    const avgPrice = document.getElementById('editSimAvg').value;
    const qty = document.getElementById('editSimQty').value;
    const targetPrice = document.getElementById('editSimTarget').value;
    const token = localStorage.getItem('jwt');
    try {
        const res = await fetch(`/api/simulator/${simId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
            body: JSON.stringify({ avgPrice: parseInt(avgPrice), quantity: parseInt(qty), targetPrice: parseInt(targetPrice) })
        });
        if (res.ok) {
            showToast('수정되었습니다!');
            await loadPortfolio();
        } else { showToast('수정에 실패했습니다.'); }
    } catch(e) { showToast('서버 오류가 발생했습니다.'); }
}

async function deleteSimulator(simId) {
    const token = localStorage.getItem('jwt');
    try {
        const res = await fetch(`/api/simulator/${simId}`, {
            method: 'DELETE',
            headers: { 'Authorization': 'Bearer ' + token }
        });
        if (res.ok) {
            showToast('삭제되었습니다.');
            await loadPortfolio();
        }
    } catch(e) { showToast('서버 오류가 발생했습니다.'); }
}

function formatFinAmt(val) {
    if (val === null || val === undefined) return 'N/A';
    const n = Math.abs(Number(val));
    const sign = Number(val) < 0 ? '-' : '';
    if (n >= 1000000000000) return sign + (n / 1000000000000).toFixed(1) + '조원';
    if (n >= 100000000) return sign + (n / 100000000).toFixed(0) + '억원';
    return sign + fmt(n) + '원';
}

async function pgAssetChart() {
    document.getElementById('mainContent').innerHTML = `<div class="page-wrap">
        <div class="page-header"><div class="page-title">📈 자산 변동 그래프</div></div>
        
        <!-- 주요 지표 -->
        <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px;margin-bottom:16px;">
        <div class="card" style="text-align:center;">
        <div style="font-size:12px;color:var(--gray);margin-bottom:8px;">현재 총 예상 자산</div>
        <div id="currentAssetDisplay" style="font-size:24px;font-weight:900;color:var(--navy);">조회 중...</div>
        </div>
        <div class="card" style="text-align:center;">
        <div style="font-size:12px;color:var(--gray);margin-bottom:8px;">금일 최고 자산</div>
        <div id="todayHighDisplay" style="font-size:24px;font-weight:900;color:var(--green);">조회 중...</div>
        </div>
        <div class="card" style="text-align:center;">
        <div style="font-size:12px;color:var(--gray);margin-bottom:8px;">금일 최저 자산</div>
        <div id="todayLowDisplay" style="font-size:24px;font-weight:900;color:var(--red-err);">조회 중...</div>
        </div>
        </div>

        <!-- 상세 통계 -->
        <div class="card mb16">
        <div class="flex flex-between flex-center" onclick="toggleAssetStats()" style="cursor:pointer;">
        <div class="section-title" style="margin-bottom:0;">📊 상세 통계</div>
        <span id="assetStatsToggle" style="font-size:12px;color:var(--blue);">▼ 펼치기</span>
        </div>
        <div id="assetStatsContent" style="display:none;margin-top:16px;">
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
        <div class="card" style="background:var(--bg);">
        <div style="font-size:11px;color:var(--gray);margin-bottom:4px;">1주일 최고 자산</div>
        <div id="weekHighDisplay" style="font-size:16px;font-weight:700;color:var(--green);">-</div>
        </div>
        <div class="card" style="background:var(--bg);">
        <div style="font-size:11px;color:var(--gray);margin-bottom:4px;">1주일 최저 자산</div>
        <div id="weekLowDisplay" style="font-size:16px;font-weight:700;color:var(--red-err);">-</div>
        </div>
        <div class="card" style="background:var(--bg);">
        <div style="font-size:11px;color:var(--gray);margin-bottom:4px;">1년 최고 자산</div>
        <div id="yearHighDisplay" style="font-size:16px;font-weight:700;color:var(--green);">-</div>
        </div>
        <div class="card" style="background:var(--bg);">
        <div style="font-size:11px;color:var(--gray);margin-bottom:4px;">1년 최저 자산</div>
        <div id="yearLowDisplay" style="font-size:16px;font-weight:700;color:var(--red-err);">-</div>
        </div>
        </div>
        </div>
        </div>

        <!-- 차트 -->
        <div class="card">
        <div class="flex flex-between flex-center mb12">
        <div class="section-title" style="margin-bottom:0;">총 자산 변동 추이</div>
        <div style="display:flex;gap:8px;">
        <button id="tabRealtime" class="btn btn-primary btn-sm" onclick="switchChartTab('realtime')">실시간</button>
        <button id="tabSampled" class="btn btn-outline btn-sm" onclick="switchChartTab('sampled')">1분 평균</button>
        </div>
        </div>
        <canvas id="assetChart" style="width:100%;height:300px;"></canvas>
        <div id="assetChartEmpty" style="display:none;text-align:center;padding:60px;color:var(--gray);">
        <div style="font-size:36px;margin-bottom:12px;">📊</div>
        <div>아직 데이터가 없습니다. 포트폴리오 페이지를 방문하면 자동으로 기록됩니다.</div>
        </div>
        </div>
        </div>`;

    const token = localStorage.getItem('jwt');
    const userEmail = state.user?.email;
    if (!userEmail) return;

    const updateStats = (data) => {
        if(!data || data.length === 0) return;
        const now = new Date();
        const todayStr = now.toISOString().slice(0, 10);
        const weekAgo = new Date(now - 7 * 24 * 60 * 60 * 1000);
        const yearAgo = new Date(now - 365 * 24 * 60 * 60 * 1000);

        const todayData = data.filter(d => d.regDt.startsWith(todayStr));
        const weekData = data.filter(d => new Date(d.regDt) >= weekAgo);
        const yearData = data.filter(d => new Date(d.regDt) >= yearAgo);

        const latest = data[data.length - 1];
        const set = (id, val) => {
            const el = document.getElementById(id);
            if(el && val !== null) el.textContent = fmt(val) + '원';
        };
        set('currentAssetDisplay', latest.totalAsset);
        set('todayHighDisplay', todayData.length ? Math.max(...todayData.map(d => d.totalAsset)) : null);
        set('todayLowDisplay', todayData.length ? Math.min(...todayData.map(d => d.totalAsset)) : null);
        set('weekHighDisplay', weekData.length ? Math.max(...weekData.map(d => d.totalAsset)) : null);
        set('weekLowDisplay', weekData.length ? Math.min(...weekData.map(d => d.totalAsset)) : null);
        set('yearHighDisplay', yearData.length ? Math.max(...yearData.map(d => d.totalAsset)) : null);
        set('yearLowDisplay', yearData.length ? Math.min(...yearData.map(d => d.totalAsset)) : null);
    };

    const sampleData = (data) => {
        const grouped = {};
        data.forEach(d => {
            const key = d.regDt.substring(0, 16); // HH:mm 단위
            if(!grouped[key]) grouped[key] = [];
            grouped[key].push(d.totalAsset);
        });
        return Object.keys(grouped).sort().map(key => ({
            regDt: key,
            totalAsset: Math.round(grouped[key].reduce((a,b) => a+b, 0) / grouped[key].length)
        }));
    };

    try {
        const res = await fetch(`/api/asset-history/${encodeURIComponent(userEmail)}`, {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        const data = await res.json();

        if (!data || data.length === 0) {
            document.getElementById('assetChart').style.display = 'none';
            document.getElementById('assetChartEmpty').style.display = 'block';
            return;
        }

        updateStats(data);

        const ctx = document.getElementById('assetChart').getContext('2d');
        const chart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.map(d => d.regDt.substring(11, 19)),
                datasets: [{
                    label: '총 자산 (원)',
                    data: data.map(d => d.totalAsset),
                    borderColor: '#1E3A5F',
                    backgroundColor: 'rgba(30,58,95,0.1)',
                    borderWidth: 2,
                    pointRadius: 2,
                    tension: 0.3,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { label: ctx => fmt(ctx.parsed.y) + '원' } }
                },
                scales: {
                    y: {
                        ticks: {
                            callback: val => {
                                if(val >= 100000000) return (val/100000000).toFixed(1) + '억';
                                if(val >= 10000000) return (val/10000000).toFixed(0) + '천만';
                                if(val >= 10000) return (val/10000).toFixed(0) + '만';
                                return fmt(val);
                            }
                        }
                    }
                }
            }
        });

        // 탭 전환 함수
        window._chartData = data;
        window._chart = chart;
        window.switchChartTab = (tab) => {
            const realtimeBtn = document.getElementById('tabRealtime');
            const sampledBtn = document.getElementById('tabSampled');
            if(tab === 'realtime') {
                realtimeBtn.className = 'btn btn-primary btn-sm';
                sampledBtn.className = 'btn btn-outline btn-sm';
                chart.data.labels = window._chartData.map(d => d.regDt.substring(11, 19));
                chart.data.datasets[0].data = window._chartData.map(d => d.totalAsset);
                chart.options.elements = { point: { radius: 2 } };
            } else {
                realtimeBtn.className = 'btn btn-outline btn-sm';
                sampledBtn.className = 'btn btn-primary btn-sm';
                const sampled = sampleData(window._chartData);
                chart.data.labels = sampled.map(d => d.regDt.substring(11, 16));
                chart.data.datasets[0].data = sampled.map(d => d.totalAsset);
            }
            chart.update();
        };

        if(window._assetChartInterval) clearInterval(window._assetChartInterval);
        window._assetChartInterval = setInterval(async () => {
            if(state.currentPage !== 'asset_chart') {
                clearInterval(window._assetChartInterval);
                window._assetChartInterval = null;
                return;
            }
            try {
                const res2 = await fetch(`/api/asset-history/${encodeURIComponent(userEmail)}`, {
                    headers: { 'Authorization': 'Bearer ' + token }
                });
                const newData = await res2.json();
                if(!newData || newData.length === 0) return;

                updateStats(newData);
                window._chartData = newData;

                const realtimeBtn = document.getElementById('tabRealtime');
                if(realtimeBtn && realtimeBtn.className.includes('btn-primary')) {
                    chart.data.labels = newData.map(d => d.regDt.substring(11, 19));
                    chart.data.datasets[0].data = newData.map(d => d.totalAsset);
                } else {
                    const sampled = sampleData(newData);
                    chart.data.labels = sampled.map(d => d.regDt.substring(11, 16));
                    chart.data.datasets[0].data = sampled.map(d => d.totalAsset);
                }
                chart.update();
            } catch(e) {}
        }, 3000);

    } catch(e) {
        console.error('자산 히스토리 조회 실패', e);
    }
}

function toggleAssetStats() {
    const content = document.getElementById('assetStatsContent');
    const toggle = document.getElementById('assetStatsToggle');
    if(content.style.display === 'none') {
        content.style.display = 'block';
        toggle.textContent = '▲ 접기';
    } else {
        content.style.display = 'none';
        toggle.textContent = '▼ 펼치기';
    }
}