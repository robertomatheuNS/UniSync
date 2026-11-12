(() => {
    "use strict";

    const storageKey = `unisync:professor:avisos:v1:${document.body.dataset.owner}`;
    const dialog = document.querySelector("#notice-dialog");
    const form = document.querySelector("#notice-form");
    const feedback = document.querySelector("#feedback");
    const list = document.querySelector("#notice-list");
    const search = document.querySelector("#search");
    const typeFilter = document.querySelector("#type-filter");
    const types = ["Atividade", "Prova", "Trabalho", "Palestra", "Evento", "Aviso"];
    const today = new Date();
    let calendarMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    let editingId = null;
    let notices = [];
    let storageAvailable = true;

    function showFeedback(message, isError = false) {
        feedback.textContent = message;
        feedback.classList.toggle("error", isError);
    }

    function isValidNotice(notice) {
        return notice && typeof notice.id === "string"
            && ["title", "subject", "teacher"].every(field =>
                typeof notice[field] === "string" && notice[field].trim().length > 0 && notice[field].length <= 100)
            && typeof notice.description === "string" && notice.description.length <= 2000
            && types.includes(notice.type)
            && typeof notice.date === "string" && /^\d{4}-\d{2}-\d{2}$/.test(notice.date)
            && !Number.isNaN(Date.parse(`${notice.date}T12:00:00`));
    }

    function readNotices() {
        const saved = JSON.parse(localStorage.getItem(storageKey) || "[]");
        if (!Array.isArray(saved) || !saved.every(isValidNotice)) {
            throw new Error("Invalid stored notices");
        }
        return saved;
    }

    function loadNotices() {
        try {
            notices = readNotices();
            storageAvailable = true;
        } catch {
            storageAvailable = false;
            showFeedback("Não foi possível ler os avisos salvos. Verifique se o armazenamento do navegador está disponível. Os dados existentes foram preservados.", true);
        }
    }

    function element(tag, className, text) {
        const node = document.createElement(tag);
        node.className = className;
        node.textContent = text;
        return node;
    }

    function daysUntil(date) {
        const target = new Date(`${date}T12:00:00`);
        const now = new Date();
        return Math.round((Date.UTC(target.getFullYear(), target.getMonth(), target.getDate())
            - Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())) / 86400000);
    }

    function dateLabel(date) {
        const days = daysUntil(date);
        if (days < 0) return "Encerrado";
        if (days === 0) return "Hoje";
        if (days === 1) return "Amanhã";
        return `Em ${days} dias`;
    }

    function formattedDate(date) {
        return new Date(`${date}T12:00:00`).toLocaleDateString("pt-BR");
    }

    function createCard(notice) {
        const card = element("article", "notice-card", "");
        const top = element("div", "card-top", "");
        top.append(element("span", "card-type", notice.type), element("span", "", dateLabel(notice.date)));
        const footer = element("div", "card-footer", "");
        const date = element("time", "", formattedDate(notice.date));
        date.dateTime = notice.date;
        const edit = element("button", "edit-button", "Editar");
        edit.type = "button";
        edit.setAttribute("aria-label", `Editar aviso: ${notice.title}`);
        edit.addEventListener("click", () => openForm(notice));
        footer.append(date, edit);
        card.append(top, element("h3", "", notice.title), element("p", "", notice.subject), element("p", "", notice.teacher));
        if (notice.description) card.append(element("p", "description", notice.description));
        card.append(footer);
        return card;
    }

    function renderNotices() {
        const query = search.value.trim().toLocaleLowerCase("pt-BR");
        const filtered = notices.filter(notice =>
            (!typeFilter.value || notice.type === typeFilter.value)
            && `${notice.title} ${notice.subject}`.toLocaleLowerCase("pt-BR").includes(query));
        list.replaceChildren(...filtered.map(createCard));
        document.querySelector("#notice-count").textContent = `${filtered.length} de ${notices.length}`;
        const empty = document.querySelector("#empty-state");
        empty.hidden = filtered.length > 0;
        empty.textContent = notices.length ? "Nenhum aviso corresponde aos filtros." : "Nenhum aviso cadastrado. Use “Novo aviso” para começar.";
        const upcoming = notices.filter(notice => daysUntil(notice.date) >= 0)
            .sort((first, second) => first.date.localeCompare(second.date)).slice(0, 6);
        document.querySelector("#reminder-list").replaceChildren(...upcoming.map(notice => {
            const item = element("li", "", notice.title);
            const date = element("time", "", `${formattedDate(notice.date)} · ${dateLabel(notice.date)}`);
            date.dateTime = notice.date;
            item.append(date);
            return item;
        }));
        document.querySelector("#reminder-empty").hidden = upcoming.length > 0;
    }

    function openForm(notice = null) {
        form.reset();
        form.querySelectorAll("input").forEach(input => input.setCustomValidity(""));
        editingId = notice?.id || null;
        document.querySelector("#dialog-title").textContent = notice ? "Editar aviso" : "Novo aviso";
        document.querySelector("#form-error").textContent = "";
        if (notice) {
            for (const field of ["title", "type", "date", "subject", "teacher", "description"]) {
                form.elements[field].value = notice[field];
            }
        }
        dialog.showModal();
        form.elements.title.focus();
    }

    function saveNotice(event) {
        event.preventDefault();
        const values = Object.fromEntries(new FormData(form));
        for (const field of ["title", "subject", "teacher"]) {
            values[field] = values[field].trim();
            form.elements[field].setCustomValidity(values[field] ? "" : "Preencha este campo.");
        }
        if (!form.reportValidity()) return;
        if (!storageAvailable) {
            document.querySelector("#form-error").textContent = "O armazenamento está indisponível. Recarregue a página após resolver o problema.";
            return;
        }
        const notice = { ...values, description: values.description.trim(), id: editingId || crypto.randomUUID() };
        try {
            const latest = readNotices();
            if (editingId && !latest.some(item => item.id === editingId)) {
                document.querySelector("#form-error").textContent = "Este aviso não está mais disponível. Feche o formulário e atualize a página.";
                return;
            }
            const updated = editingId ? latest.map(item => item.id === editingId ? notice : item) : [notice, ...latest];
            localStorage.setItem(storageKey, JSON.stringify(updated));
            notices = updated;
        } catch {
            document.querySelector("#form-error").textContent = "Não foi possível salvar. Verifique o espaço e as permissões de armazenamento do navegador e tente novamente.";
            return;
        }
        search.value = "";
        typeFilter.value = "";
        renderNotices();
        dialog.close();
        showFeedback(editingId ? "Aviso atualizado neste navegador." : "Aviso cadastrado neste navegador.");
    }

    function renderCalendar() {
        document.querySelector("#calendar-title").textContent = calendarMonth.toLocaleDateString("pt-BR", { month: "long", year: "numeric" });
        const year = calendarMonth.getFullYear();
        const month = calendarMonth.getMonth();
        const offset = calendarMonth.getDay();
        const total = new Date(year, month + 1, 0).getDate();
        const rows = [];
        for (let week = 0; week < Math.ceil((offset + total) / 7); week++) {
            const row = document.createElement("tr");
            for (let weekday = 0; weekday < 7; weekday++) {
                const day = week * 7 + weekday - offset + 1;
                const cell = document.createElement("td");
                if (day > 0 && day <= total) {
                    cell.textContent = day;
                    if (day === today.getDate() && month === today.getMonth() && year === today.getFullYear()) {
                        cell.className = "today";
                        cell.setAttribute("aria-current", "date");
                    }
                }
                row.append(cell);
            }
            rows.push(row);
        }
        document.querySelector("#calendar-days").replaceChildren(...rows);
    }

    document.querySelector("#new-notice").addEventListener("click", () => openForm());
    document.querySelector("#close-dialog").addEventListener("click", () => dialog.close());
    document.querySelector("#cancel-dialog").addEventListener("click", () => dialog.close());
    form.addEventListener("submit", saveNotice);
    form.addEventListener("input", event => event.target.setCustomValidity(""));
    document.querySelector("#filters").addEventListener("submit", event => event.preventDefault());
    search.addEventListener("input", renderNotices);
    typeFilter.addEventListener("change", renderNotices);
    document.querySelector("#previous-month").addEventListener("click", () => {
        calendarMonth.setMonth(calendarMonth.getMonth() - 1);
        renderCalendar();
    });
    document.querySelector("#next-month").addEventListener("click", () => {
        calendarMonth.setMonth(calendarMonth.getMonth() + 1);
        renderCalendar();
    });
    window.addEventListener("storage", event => {
        if (event.key === storageKey || event.key === null) {
            loadNotices();
            renderNotices();
        }
    });
    loadNotices();
    renderNotices();
    renderCalendar();
})();
