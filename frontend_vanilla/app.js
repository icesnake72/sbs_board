const API_BOARDS = "/api/board/all"

const listEl = document.getElementById("board-list");
const statusEl = document.getElementById("status");
const countEl = document.getElementById("count");
const refreshBtn = document.getElementById("refresh");


function formatDate(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;

    const p = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}.${p(d.getMonth()+1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function renderBoards(boards) {
    listEl.replaceChildren();

    for(const b of boards) {
        const li = document.createElement("li");
        li.className = "board-card";

        const id = document.createElement("span");
        id.className = "board-id";
        id.textContent = `#${b.id}`

        const body = document.createElement("div");
        body.className = "board-body";

        const name = document.createElement("p");
        name.className = "board-name";
        name.textContent = b.name ?? "(이름없음)";

        const desc = document.createElement("p");
        desc.className = "board-desc";
        desc.textContent = b.description ?? "";

        body.append(name, desc);

        const date = document.createElement("time");
        date.className = "board-date";
        date.dateTime = b.createdAt ?? "";
        date.textContent = formatDate(b.createdAt);

        li.append(id, body, date);
        listEl.append(li);
    }
}

function setStatus(message, isError = false) {
    statusEl.textContent = message;
    statusEl.classList.toggle("error", isError);
}

// 비동기 함수:
async function loadBoards() {
    setStatus("불러오는 중...");
    countEl.textContent = "";
    refreshBtn.disabled = true;

    try {
        const res = await fetch(API_BOARDS,
            {
                    headers: {
                                Accept: "application/json"
                    }
            }
        );

        if(!res.ok) {
            throw new Error(`서버 응답 오류 HTTP ${res.status}`)
        }

        const boards = await res.json();
        if (!Array.isArray(boards) || boards.length === 0) {
            listEl.replaceChildren();
            setStatus("등록된 게시판이 없습니다");
            countEl.textContent = "0개";
            return;
        }

        setStatus("");
        renderBoards(boards);
        countEl.textContent = `${boards.length}개`
    } catch (err) {
        listEl.replaceChildren();
        setStatus(`목록을 불러오지 못 했습니다: ${err.message}`)
    } finally {
        refreshBtn.disabled = false;
    }
}

refreshBtn.addEventListener("click", loadBoards);
loadBoards();
