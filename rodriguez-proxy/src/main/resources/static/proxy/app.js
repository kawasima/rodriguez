const API_BASE = '/_proxy/api';
const pathPatterns = new Set();
const activeRules = new Map();

async function init() {
    const [behaviors, rules, paths] = await Promise.all([
        fetch(`${API_BASE}/behaviors`).then(r => r.json()),
        fetch(`${API_BASE}/rules`).then(r => r.json()),
        fetch(`${API_BASE}/paths`).then(r => r.json()),
    ]);

    renderBehaviorCards(behaviors);

    paths.forEach(path => pathPatterns.add(path));
    rules.forEach(rule => {
        pathPatterns.add(rule.pathPattern);
        activeRules.set(rule.id, rule);
    });

    renderPaths();
    connectSSE();
}

function renderBehaviorCards(behaviors) {
    const container = document.getElementById('cards-container');
    behaviors.forEach(b => {
        const card = document.createElement('div');
        card.className = 'fault-card';
        card.draggable = true;
        card.dataset.faultType = b.name;
        card.dataset.faultPort = b.port;
        card.innerHTML = `
            <div class="name">${b.name}</div>
            <div class="port">port: ${b.port}</div>
            <div class="desc">${b.description}</div>
        `;
        card.addEventListener('dragstart', (e) => {
            e.dataTransfer.setData('application/json', JSON.stringify({
                faultType: b.name,
                faultPort: b.port,
            }));
        });
        container.appendChild(card);
    });
}

function renderPaths() {
    const container = document.getElementById('paths-container');
    container.innerHTML = '';
    pathPatterns.forEach(pattern => {
        const row = document.createElement('div');
        row.className = 'path-row';
        row.dataset.pathPattern = pattern;

        const patternEl = document.createElement('span');
        patternEl.className = 'pattern';
        patternEl.textContent = pattern;
        row.appendChild(patternEl);

        activeRules.forEach(rule => {
            if (rule.pathPattern === pattern) {
                const badge = createRuleBadge(rule);
                row.appendChild(badge);
            }
        });

        row.addEventListener('dragover', (e) => {
            e.preventDefault();
            row.classList.add('drag-over');
        });
        row.addEventListener('dragleave', () => {
            row.classList.remove('drag-over');
        });
        row.addEventListener('drop', (e) => {
            e.preventDefault();
            row.classList.remove('drag-over');
            const fault = JSON.parse(e.dataTransfer.getData('application/json'));
            registerFault(pattern, fault);
        });

        container.appendChild(row);
    });
}

function createRuleBadge(rule) {
    const badge = document.createElement('span');
    badge.className = 'rule-badge';
    badge.dataset.ruleId = rule.id;
    badge.innerHTML = `${rule.faultType} <span class="count">&times;${rule.remaining}</span>`;
    badge.title = 'Click to increment';
    badge.addEventListener('click', async () => {
        await fetch(`${API_BASE}/rules/${rule.id}/increment`, { method: 'PATCH' });
    });
    return badge;
}

async function registerFault(pathPattern, fault) {
    await fetch(`${API_BASE}/rules`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            pathPattern,
            faultType: fault.faultType,
            faultPort: fault.faultPort,
            count: 1,
        }),
    });
}

document.getElementById('add-path-btn').addEventListener('click', () => {
    const input = document.getElementById('custom-path-input');
    const pattern = input.value.trim();
    if (pattern) {
        pathPatterns.add(pattern);
        renderPaths();
        input.value = '';
    }
});

function connectSSE() {
    const eventSource = new EventSource('/_proxy/events');

    eventSource.addEventListener('rule-added', (e) => {
        const data = JSON.parse(e.data);
        activeRules.set(data.ruleId, {
            id: data.ruleId,
            pathPattern: data.pathPattern,
            faultType: data.faultType,
            remaining: data.remaining,
        });
        pathPatterns.add(data.pathPattern);
        renderPaths();
        appendLog('rule-added',
            `Rule added: ${data.faultType} on ${data.pathPattern} (\u00d7${data.remaining})`);
    });

    eventSource.addEventListener('rule-consumed', (e) => {
        const data = JSON.parse(e.data);
        const rule = activeRules.get(data.ruleId);
        if (rule) {
            rule.remaining = data.remaining;
            renderPaths();
        }
        appendLog('rule-consumed',
            `Consumed: ${data.faultType} on ${data.pathPattern} (${data.remaining} left)`);
    });

    eventSource.addEventListener('rule-removed', (e) => {
        const data = JSON.parse(e.data);
        activeRules.delete(data.ruleId);
        renderPaths();
        appendLog('rule-removed',
            `Rule removed: ${data.faultType} on ${data.pathPattern}`);
    });

    eventSource.addEventListener('path-observed', (e) => {
        const data = JSON.parse(e.data);
        if (!pathPatterns.has(data.path)) {
            pathPatterns.add(data.path);
            renderPaths();
            appendLog('path-observed', `Path discovered: ${data.path}`);
        }
    });

    eventSource.onerror = () => {
        appendLog('rule-removed', 'SSE disconnected, reconnecting...');
    };
}

function appendLog(type, message) {
    const container = document.getElementById('events-container');
    const entry = document.createElement('div');
    entry.className = `event-entry ${type}`;
    const now = new Date().toLocaleTimeString('en-US', { hour12: false });
    entry.innerHTML = `<span class="time">${now}</span> <span class="msg">${message}</span>`;
    container.prepend(entry);

    while (container.children.length > 100) {
        container.removeChild(container.lastChild);
    }
}

init();
