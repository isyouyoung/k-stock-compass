async function pgStockIndex(){
    document.getElementById('mainContent').innerHTML = `<div class="page-wrap">
        <div class="page-header"><div class="page-title">📊 시장 지수 조회</div><div class="page-sub">공공데이터포털 API 기반 전일 종가 기준</div></div>
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
        <div class="notice mb16">ℹ 본 시세 정보는 공공데이터포털 API 기반 전일 종가 기준입니다.</div>
        </div>`;

    const getBaseDate = () => {
        const d = new Date();
        d.setDate(d.getDate() - 1);
        return d.toISOString().slice(0,10).replace(/-/g,'');
    };
    const today = getBaseDate();
    const token = localStorage.getItem('jwt');
    const headers = token ? {'Authorization': 'Bearer ' + token} : {};

    try {
        const [kospiRes, kosdaqRes] = await Promise.all([
            fetch(`/api/stock/index?idxNm=코스피&baseDate=${today}`, {headers}),
            fetch(`/api/stock/index?idxNm=코스닥&baseDate=${today}`, {headers})
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

        document.getElementById('kospiCard').innerHTML = `<div style="font-size:14px;font-weight:700;color:var(--navy);margin-bottom:12px;">KOSPI 코스피</div>${renderCard(kospi)}`;
        document.getElementById('kosdaqCard').innerHTML = `<div style="font-size:14px;font-weight:700;color:var(--blue);margin-bottom:12px;">KOSDAQ 코스닥</div>${renderCard(kosdaq)}`;

        document.getElementById('mainContent').innerHTML += `
        <div class="page-wrap" style="padding-top:0;">
        <div class="card">
        <div class="section-title">주요 종목 시세</div>
        ${[['삼성전자','005930','KOSPI',61200,1200,2.00],['SK하이닉스','000660','KOSPI',182500,-1500,-0.81],['LG에너지솔루션','373220','KOSPI',415000,3000,0.73],['NAVER','035420','KOSPI',215000,3000,1.41],['카카오','035720','KOSDAQ',45800,-200,-0.43]].map(([n,c,m,p,ch,pct])=>`
        <div class="stock-row" onclick="navigate('stock_detail',{currentStock:'${c}'})">
        <span class="sn">${n}</span><span class="sc">${c}</span>
        <span class="badge badge-${m.toLowerCase()}">${m}</span>
        <span class="sp ${ch>=0?'up':'down'}" style="margin-left:auto;">${fmt(p)}원</span>
        <span class="sch ${ch>=0?'up':'down'}">${ch>=0?'▲ +':'▼ -'}${fmt(Math.abs(ch))} (${pct>=0?'+':''}${pct.toFixed(2)}%)</span>
        </div>`).join('')}
        </div></div>`;

    } catch(e) {
        showToast('시장 지수 조회에 실패했습니다.');
    }
}

function pgStockSearch(){
    const stocks=[
        {code:'005930',name:'삼성전자',market:'KOSPI',price:61200,change:1200,pct:2.00},
        {code:'000660',name:'SK하이닉스',market:'KOSPI',price:182500,change:-1500,pct:-0.81},
        {code:'373220',name:'LG에너지솔루션',market:'KOSPI',price:415000,change:3000,pct:0.73},
        {code:'035420',name:'NAVER',market:'KOSPI',price:215000,change:3000,pct:1.41},
        {code:'035720',name:'카카오',market:'KOSDAQ',price:45800,change:-200,pct:-0.43},
        {code:'005380',name:'현대차',market:'KOSPI',price:231000,change:2500,pct:1.09},
        {code:'006400',name:'삼성SDI',market:'KOSPI',price:298500,change:-2500,pct:-0.83},
        {code:'207940',name:'삼성바이오로직스',market:'KOSPI',price:781000,change:5000,pct:0.64},
    ];
    window._allStocks = stocks;
    return `<div class="page-wrap">
        <div class="page-header"><div class="page-title">🔍 종목 검색</div></div>
        <div class="card mb16">
        <div class="search-bar mb12"><input id="stockSearchInput" placeholder="종목명 또는 종목코드 입력 (예: 삼성전자, 005930)" oninput="doStockSearch()" onkeydown="if(event.key==='Enter')doStockSearch()"><button onclick="doStockSearch()">🔍 검색</button></div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
        ${['삼성전자','SK하이닉스','NAVER','카카오'].map(k=>`<span class="tag" onclick="document.getElementById('stockSearchInput').value='${k}';doStockSearch()">${k}</span>`).join('')}
        </div></div>
        <div id="searchResults">
        <div class="card"><div class="section-title">인기 종목 TOP 5</div>
        ${stocks.slice(0,5).map((s,i)=>`
        <div class="stock-row" onclick="navigate('stock_detail',{currentStock:'${s.code}'})">
        <span style="font-size:16px;font-weight:700;color:${i<3?'var(--blue)':'var(--gray)'};min-width:28px;">${i+1}</span>
        <span class="sn">${s.name}</span><span class="sc">${s.code}</span>
        <span class="badge badge-${s.market.toLowerCase()}">${s.market}</span>
        <span class="sp ${s.change>=0?'up':'down'}" style="margin-left:auto;">${fmt(s.price)}원</span>
        <span class="sch ${s.change>=0?'up':'down'}">${s.change>=0?'▲ +':'▼ -'}${fmt(Math.abs(s.change))} (${s.pct>=0?'+':''}${s.pct.toFixed(2)}%)</span>
        </div>`).join('')}
        </div></div></div>`;}

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

        container.innerHTML=`<div class="card"><div style="font-size:13px;color:var(--gray);margin-bottom:16px;">'${q}' 검색 결과 <strong style="color:var(--navy);">${results.length}건</strong></div>
        ${results.map(s=>`
        <div class="stock-row" onclick="navigate('stock_detail',{currentStock:'${s.stockCd}'})">
        <span class="sn">${s.stockNm}</span>
        <span class="sc">${s.stockCd}</span>
        ${state.loggedIn?`<button class="btn btn-ghost btn-sm" style="margin-left:auto;" onclick="event.stopPropagation();openFavAdd('${s.stockNm}','${s.stockCd}')">⭐</button>`:''}
        </div>`).join('')}</div>`;

    } catch(e) {
        container.innerHTML=`<div class="card"><div style="color:var(--red-err);font-size:13px;">검색 중 오류가 발생했습니다.</div></div>`;
    }
}

function doMainSearch(){
    const q=document.getElementById('mainSearch').value;
    navigate('stock_search');
    setTimeout(()=>{const el=document.getElementById('stockSearchInput');if(el){el.value=q;doStockSearch();}},50);
}

function pgStockDetail(){
    const stockMap={
        '005930':{code:'005930',name:'삼성전자',market:'KOSPI',price:61200,change:1200,pct:2.00},
        '000660':{code:'000660',name:'SK하이닉스',market:'KOSPI',price:182500,change:-1500,pct:-0.81},
        '035420':{code:'035420',name:'NAVER',market:'KOSPI',price:215000,change:3000,pct:1.41},
        '035720':{code:'035720',name:'카카오',market:'KOSDAQ',price:45800,change:-200,pct:-0.43},
        '373220':{code:'373220',name:'LG에너지솔루션',market:'KOSPI',price:415000,change:3000,pct:0.73},
        '005380':{code:'005380',name:'현대차',market:'KOSPI',price:231000,change:2500,pct:1.09},
        '006400':{code:'006400',name:'삼성SDI',market:'KOSPI',price:298500,change:-2500,pct:-0.83},
        '207940':{code:'207940',name:'삼성바이오로직스',market:'KOSPI',price:781000,change:5000,pct:0.64},
    };
    const s=stockMap[state.currentStock]||stockMap['005930'];
    const isFav=state.favorites.some(f=>f.code===s.code);
    const tab=state.detailTab||'info';
    return `
        <div class="detail-header">
        <div><div class="detail-name">${s.name}</div><div class="detail-meta">${s.code} &nbsp;|&nbsp; <span class="badge badge-${s.market.toLowerCase()}">${s.market}</span></div></div>
        <div class="detail-price">
        <div class="price">${fmt(s.price)}원</div>
        <div class="change ${s.change>=0?'up':'down'}">${s.change>=0?'▲ +':'▼ -'}${fmt(Math.abs(s.change))} &nbsp;(${s.pct>=0?'+':''}${s.pct.toFixed(2)}%)</div>
        </div></div>
        <div class="inner-tabs">
        <div class="inner-tab ${tab==='info'?'active':''}" onclick="navigate('stock_detail',{currentStock:'${s.code}',detailTab:'info'})">종목 정보</div>
        <div class="inner-tab ${tab==='ai'?'active':''}" onclick="navigate('stock_detail',{currentStock:'${s.code}',detailTab:'ai'})">AI 신호등</div>
        </div>
        <div class="page-wrap">
        ${tab==='info'?detailInfo(s,isFav):detailAI(s)}
        </div>`;}

function detailInfo(s,isFav){return `
        <div class="grid3 mb16">
        ${[['현재가',`${fmt(s.price)}원`],['전일 대비',`${s.change>=0?'▲ +':'▼ -'}${fmt(Math.abs(s.change))}`],['등락률',`${s.pct>=0?'+':''}${s.pct.toFixed(2)}%`],['시가','60,300원'],['고가','61,500원'],['저가','60,100원'],['거래량','12,345,678주'],['시가총액','365조 4,321억'],['52주 최고','87,800원']].map(([l,v])=>`
        <div class="card"><div style="font-size:11px;color:var(--gray);margin-bottom:6px;">${l}</div><div style="font-size:16px;font-weight:700;">${v}</div></div>`).join('')}
        </div>
        <div class="card mb16">
        <div class="flex flex-between flex-center mb12">
        <div><div class="section-title" style="margin-bottom:2px;">기업 재무 정보</div><div style="font-size:12px;color:var(--gray);">DART 전자공시 API · 최근 분기 기준</div></div>
        </div>
        <div class="grid3">
        ${[['부채비율','38.2%','업종평균 72.1%','var(--green)'],['영업이익률','15.3%','전년동기 12.8%','var(--green)'],['유동비율','218%','업종평균 145%','var(--green)'],['ROE','12.5%','전년동기 9.2%','var(--blue)'],['매출액','79.1조원','YoY +8.3%','var(--dark)'],['영업이익','12.1조원','YoY +19.5%','var(--dark)']].map(([l,v,s2,c])=>`
        <div class="fin-card"><div class="fin-bar" style="background:${c};"></div><div class="fin-label">${l}</div><div class="fin-val" style="color:${c};">${v}</div><div class="fin-sub">${s2}</div></div>`).join('')}
        </div></div>
        <div class="flex gap12">
        ${state.loggedIn?`
        <button class="btn ${isFav?'btn-secondary':'btn-outline'} btn-full" onclick="openFavAdd('${s.name}','${s.code}')">${isFav?'✅ 관심종목 추가됨':'⭐ 관심종목 추가'}</button>
        <button class="btn btn-primary btn-full" onclick="navigate('alert_add',{alertStock:{code:'${s.code}',name:'${s.name}',price:${s.price}}})">🔔 알림 설정</button>`
    :`<div class="notice btn-full" style="text-align:center;">로그인 후 관심종목 추가 및 알림 설정이 가능합니다. <span style="color:var(--blue);cursor:pointer;font-weight:600;" onclick="navigate('login')">로그인 →</span></div>`}
        </div>`;}

function detailAI(s){
    if(!state._aiMessages) state._aiMessages={};
    if(!state._aiMessages[s.code]) state._aiMessages[s.code]=[
        {role:'ai',text:`안녕하세요! ${s.name} 재무 분석 AI입니다. 궁금한 점을 질문해 주세요 😊`}
    ];
    const msgs=state._aiMessages[s.code];
    return `
        <div class="card mb16">
        <div class="flex flex-between flex-center mb12">
        <div><div class="section-title" style="margin-bottom:2px;">AI 재무 신호등</div>
        <div style="font-size:12px;color:var(--gray);">OpenAI 기반 재무 안정성 분석</div></div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px;align-items:center;">
        <div>
        <div class="signal-wrap mb12">
        ${[['#DC2626','1점','매우위험',false],['#F97316','2점','위험',false],['#EAB308','3점','보통',false],['#22C55E','4점','양호',true],['#16A34A','5점','매우안전',false]].map(([c,sc,lb,act])=>`
        <div><div class="signal-dot ${act?'big':'small'}" style="background:${act?c:'#D1D5DB'};">${sc}</div>
        <div class="signal-label" style="color:${act?'var(--dark)':'var(--gray)'};font-weight:${act?'700':'400'};">${lb}</div></div>`).join('')}
        </div>
        <div class="success-bar" style="font-size:13px;font-weight:600;">종합 평가: 양호 (4점/5점) — 재무 안정성이 우수한 기업입니다</div>
        </div>
        <div style="background:var(--bg);border-radius:10px;padding:20px;height:100%;box-sizing:border-box;">
        <div style="font-size:13px;font-weight:700;color:var(--navy);margin-bottom:10px;">📋 재무 분석 요약</div>
        <div style="font-size:13px;color:var(--dark);line-height:1.9;">
        최근 3개년(2022~2024) 기준으로 <strong>부채비율이 꾸준히 감소</strong>하는 추세이며, 재무 건전성이 개선되고 있습니다.<br><br>
        영업이익률은 전년 대비 <strong>2.5%p 상승</strong>하여 수익성이 향상되고 있는 상황이며, 유동비율 218%로 단기 채무 상환 능력 또한 우수한 수준을 유지하고 있습니다.<br><br>
        전반적으로 <strong>안정적인 재무 구조</strong>를 유지하면서 성장세를 이어가고 있어 긍정적으로 평가됩니다.
        </div>
        <div style="font-size:11px;color:var(--gray);margin-top:12px;">분석 기준: 2026.03.24 · DART 공시 기반 · Redis 캐싱 적용</div>
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
        <div style="font-size:11px;color:var(--gray);margin-top:6px;">※ AI 응답은 데모 버전입니다.</div>
        </div>`;}

function sendAiMsg(code, name){
    const input=document.getElementById(`aiInput_${code}`);
    if(!input) return;
    const text=input.value.trim();
    if(!text) return;
    if(!state._aiMessages) state._aiMessages={};
    if(!state._aiMessages[code]) state._aiMessages[code]=[];
    state._aiMessages[code].push({role:'user',text});
    input.value='';
    const responses=[
        `${name}의 재무 안정성은 전반적으로 양호한 수준입니다.`,
        `최근 ${name}의 영업이익률은 15.3%로 업종 평균 대비 높은 편입니다.`,
        `${name}의 ROE는 12.5%로 적정 수준입니다.`,
        `DART 공시 데이터 기준으로 ${name}의 재무제표를 분석한 결과, 건전한 재무 구조를 유지하고 있습니다.`,
    ];
    const reply=responses[Math.floor(Math.random()*responses.length)];
    setTimeout(()=>{
        state._aiMessages[code].push({role:'ai',text:reply});
        navigate('stock_detail',{currentStock:code,detailTab:'ai'});
    },800);
    navigate('stock_detail',{currentStock:code,detailTab:'ai'});
}