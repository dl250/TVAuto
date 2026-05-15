const listEl = document.querySelector("#channel-list");
const countEl = document.querySelector("#channel-count");
const toastEl = document.querySelector("#toast");
const addForm = document.querySelector("#add-form");
const nameInput = document.querySelector("#name-input");
const urlInput = document.querySelector("#url-input");
const fileInput = document.querySelector("#file-input");
const importText = document.querySelector("#import-text");
const importButton = document.querySelector("#import-button");
const restoreButton = document.querySelector("#restore-button");
const clearButton = document.querySelector("#clear-button");
const scriptForm = document.querySelector("#script-form");
const scriptSiteInput = document.querySelector("#script-site-input");
const scriptCodeInput = document.querySelector("#script-code-input");
const scriptFileInput = document.querySelector("#script-file-input");
const scriptImportButton = document.querySelector("#script-import-button");
const scriptListEl = document.querySelector("#script-list");
const scriptCountEl = document.querySelector("#script-count");

async function api(path, options) {
    const response = await fetch(path, options);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
}

function toast(message) {
    toastEl.textContent = message;
}

async function loadChannels() {
    const data = await api("/api/channels");
    const channels = data.channels ?? [];
    countEl.textContent = `${channels.length} 个`;
    clearButton.disabled = channels.length === 0;
    listEl.innerHTML = "";

    channels.forEach((channel) => {
        const row = document.createElement("div");
        row.className = "channel-row";
        row.innerHTML = `
            <div>
                <strong></strong>
                <small></small>
            </div>
            <button class="delete-button" type="button">删除</button>
        `;
        row.querySelector("strong").textContent = channel.name;
        row.querySelector("small").textContent = channel.url;
        row.querySelector("button").addEventListener("click", async () => {
            await api("/api/delete", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({url: channel.url}),
            });
            toast("已删除");
            await loadChannels();
        });
        listEl.appendChild(row);
    });
}

async function loadScripts() {
    const data = await api("/api/scripts");
    const scripts = data.scripts ?? [];
    scriptCountEl.textContent = `${scripts.length} 个`;
    scriptListEl.innerHTML = "";

    scripts.forEach((script) => {
        const row = document.createElement("div");
        row.className = "script-row";
        row.innerHTML = `
            <div>
                <strong></strong>
                <small></small>
            </div>
            <div class="script-actions">
                <button class="secondary-button edit-script-button" type="button">编辑</button>
                <button class="delete-button" type="button">删除</button>
            </div>
        `;
        row.querySelector("strong").textContent = script.sitePattern;
        row.querySelector("small").textContent = `脚本长度：${script.javascript.length} 字符`;
        row.querySelector(".edit-script-button").addEventListener("click", () => {
            scriptSiteInput.value = script.sitePattern;
            scriptCodeInput.value = script.javascript;
            scriptSiteInput.focus();
            toast("已载入脚本，可直接修改后保存");
        });
        row.querySelector(".delete-button").addEventListener("click", async () => {
            await api("/api/scripts/delete", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({sitePattern: script.sitePattern}),
            });
            toast("脚本已删除");
            await loadScripts();
        });
        scriptListEl.appendChild(row);
    });
}

addForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const result = await api("/api/channels", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            name: nameInput.value.trim(),
            url: urlInput.value.trim(),
        }),
    });
    toast(result.added ? "已新增" : "频道已存在");
    if (result.added) addForm.reset();
    await loadChannels();
});

fileInput.addEventListener("change", async () => {
    const file = fileInput.files?.[0];
    if (!file) return;
    importText.value = await file.text();
});

importButton.addEventListener("click", async () => {
    const result = await api("/api/import", {
        method: "POST",
        headers: {"Content-Type": "text/plain; charset=UTF-8"},
        body: importText.value,
    });
    toast(result.imported > 0 ? `已导入 ${result.imported} 个频道` : "没有可导入的新频道");
    await loadChannels();
});

clearButton.addEventListener("click", async () => {
    if (!window.confirm("确定删除全部频道吗？")) return;

    const result = await api("/api/clear", {method: "POST"});
    toast(result.deleted > 0 ? `已删除 ${result.deleted} 个频道` : "当前没有可删除的频道");
    await loadChannels();
});

restoreButton.addEventListener("click", async () => {
    if (!window.confirm("将用内置默认频道替换当前列表，确定继续吗？")) return;

    const result = await api("/api/defaults", {method: "POST"});
    toast(result.restored > 0 ? `已恢复 ${result.restored} 个默认频道` : "默认频道不可用");
    await loadChannels();
});

scriptForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    await api("/api/scripts", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            sitePattern: scriptSiteInput.value.trim(),
            javascript: scriptCodeInput.value,
        }),
    });
    toast("脚本已保存");
    scriptForm.reset();
    await loadScripts();
});

scriptImportButton.addEventListener("click", async () => {
    const file = scriptFileInput.files?.[0];
    if (!file) {
        toast("请先选择脚本 JSON 文件");
        return;
    }

    try {
        const result = await api("/api/scripts/import", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: await file.text(),
        });
        toast(`已新增 ${result.added} 个脚本，更新 ${result.updated} 个脚本`);
        scriptFileInput.value = "";
        await loadScripts();
    } catch (error) {
        toast("脚本导入失败，请检查 JSON 文件");
    }
});

Promise.all([loadChannels(), loadScripts()])
    .catch(() => toast("加载失败，请确认电视端管理窗口仍然打开"));
