let user = layUserDaLuu();
let gioHangDangSuaKey = null;
let gheDangChon = [];

capNhatTrangThaiDangNhap();
kiemTraDangNhap();
renderGioHang();

function layUserDaLuu() {
    try {
        return JSON.parse(localStorage.getItem("user"));
    } catch (e) {
        localStorage.removeItem("user");
        return null;
    }
}

function capNhatTrangThaiDangNhap() {
    const authButton = document.getElementById("authButton");
    const profileButton = document.getElementById("profileButton");

    if (authButton) {
        authButton.innerText = user ? "Đăng xuất" : "Đăng nhập";
    }

    if (profileButton) {
        profileButton.classList.toggle("hidden", !user);
    }
}

function kiemTraDangNhap() {
    fetch("/nguoidung/me", {
        credentials: "include"
    })
        .then(async res => {
            if (!res.ok) {
                localStorage.removeItem("user");
                user = null;
                capNhatTrangThaiDangNhap();
                renderGioHang();
                return;
            }

            user = await res.json();
            localStorage.setItem("user", JSON.stringify(user));
            capNhatTrangThaiDangNhap();
            renderGioHang();
        })
        .catch(() => {
            capNhatTrangThaiDangNhap();
        });
}

function renderGioHang() {
    const container = document.getElementById("gioHangList");
    const gioHang = layGioHang();
    const tongSoVe = gioHang.reduce((tong, item) => tong + (item.maGheList?.length || 0), 0);
    const tongTien = gioHang.reduce((tong, item) => tong + Number(item.tongTien || 0), 0);

    document.getElementById("tongSoVe").innerText = tongSoVe;
    document.getElementById("tongTienGioHang").innerText = formatTien(tongTien);
    document.getElementById("btnThanhToanTatCa").disabled = gioHang.length === 0;
    capNhatGioHangBadge(tongSoVe);

    if (!container) {
        return;
    }

    if (gioHang.length === 0) {
        container.innerHTML = `
            <div class="cart-empty">
                <h2>Giỏ hàng trống</h2>
                <p>Chọn phim và ghế để thêm vé vào giỏ hàng.</p>
                <button class="btn-them" onclick="window.location.href='/index.html#dsPhim'">
                    Chọn phim
                </button>
            </div>
        `;
        return;
    }

    container.innerHTML = gioHang.map(item => `
        <article class="cart-item cart-item-large">
            <div>
                <h3>${escapeHtml(item.tenPhim || "Phim")}</h3>
                <p>${formatNgayGio(item.thoiGianBatDau)}${item.tenPhong ? ` - ${escapeHtml(item.tenPhong)}` : ""}</p>
                <p>Ghế: <strong>${(item.gheList || []).map(ghe => escapeHtml(ghe.soGhe)).join(", ")}</strong></p>
                <p class="cart-price">${formatTien(item.tongTien)}</p>
            </div>
            <div class="cart-actions">
                <button class="btn-sua" onclick="moDoiGhe('${item.key}')">Đổi ghế</button>
                <button class="btn-xoa" onclick="xoaKhoiGioHang('${item.key}')">Xóa</button>
            </div>
        </article>
    `).join("");
}

function moDoiGhe(key) {
    const item = layGioHang().find(cartItem => cartItem.key === key);

    if (!item) {
        return;
    }

    gioHangDangSuaKey = key;
    gheDangChon = [...(item.maGheList || [])];

    fetch(`/datve/ghe/${item.maSuatChieu}`, {
        credentials: "include"
    })
        .then(async res => {
            if (!res.ok) {
                throw new Error(await res.text() || "Không tải được ghế");
            }

            return res.json();
        })
        .then(data => {
            document.getElementById("dsGhe").innerHTML = renderSoDoGhe(data);
            gheDangChon = gheDangChon.filter(maGhe => {
                const gheButton = document.querySelector(`[data-ma-ghe="${maGhe}"]`);

                if (gheButton && !gheButton.disabled) {
                    gheButton.classList.add("selected");
                    return true;
                }

                return false;
            });
            capNhatTamTinhGhe();
            document.getElementById("seatModal").classList.remove("hidden");
        })
        .catch(err => {
            alert(err.message || "Không tải được ghế");
        });
}

function luuDoiGhe() {
    if (!gioHangDangSuaKey || gheDangChon.length === 0) {
        alert("Vui lòng chọn ghế");
        return;
    }

    const gioHang = layGioHang();
    const index = gioHang.findIndex(item => item.key === gioHangDangSuaKey);

    if (index < 0) {
        dongSeatModal();
        return;
    }

    const gheList = gheDangChon.map(maGhe => {
        const gheButton = document.querySelector(`[data-ma-ghe="${maGhe}"]`);
        return {
            maGhe,
            soGhe:gheButton?.getAttribute("title")?.split(" - ")[0] || String(maGhe),
            gia:layGiaGhe(gheButton)
        };
    });

    gioHang[index] = {
        ...gioHang[index],
        gheList,
        maGheList:gheList.map(ghe => ghe.maGhe),
        tongTien:gheList.reduce((tong, ghe) => tong + ghe.gia, 0)
    };

    luuGioHang(gioHang);
    dongSeatModal();
    renderGioHang();
}

function thanhToanTatCa() {
    if (!user) {
        alert("Vui lòng đăng nhập để thanh toán");
        window.location.href = "login.html";
        return;
    }

    const gioHang = layGioHang().filter(item =>
        item.maSuatChieu && Array.isArray(item.maGheList) && item.maGheList.length > 0
    );

    if (gioHang.length === 0) {
        alert("Giỏ hàng chưa có vé");
        return;
    }

    fetch("/payment/create", {
        method: "POST",
        credentials: "include",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            items: gioHang.map(item => ({
                maSuatChieu: item.maSuatChieu,
                maGheList: item.maGheList
            }))
        })
    })
        .then(async res => {
            if (!res.ok) {
                if (res.status === 401) {
                    localStorage.removeItem("user");
                    alert("Vui lòng đăng nhập để thanh toán");
                    window.location.href = "login.html";
                    throw new Error("REDIRECT_LOGIN");
                }

                const contentType = res.headers.get("content-type") || "";
                if (contentType.includes("application/json")) {
                    const data = await res.json();
                    throw new Error(data.message || "Không tạo được thanh toán");
                }

                throw new Error(await res.text() || "Không tạo được thanh toán");
            }

            return res.json();
        })
        .then(data => {
            luuGioHang([]);
            window.location.href = data.paymentUrl;
        })
        .catch(err => {
            if (err.message !== "REDIRECT_LOGIN") {
                alert(err.message || "Không tạo được thanh toán");
            }
        });
}

function renderSoDoGhe(data) {
    if (data.length === 0) {
        return `<p class="muted">Phòng này chưa có ghế</p>`;
    }

    const gheTheoSo = {};
    data.forEach(ghe => {
        gheTheoSo[ghe.soGhe] = ghe;
    });

    const hangGhe = ["A", "B", "C", "D", "E", "F"];
    const soCot = 12;
    let html = `
        <div class="seat-map">
            <div class="screen-line"></div>
            <div class="screen-title">Màn hình chính</div>
            <div class="column-labels">
                <div></div>
                ${Array.from({ length: soCot }, (_, i) => `<span class="seat-column seat-column-${i + 1}">${i + 1}</span>`).join("")}
            </div>
            <div class="seat-map-body">
    `;

    hangGhe.forEach(hang => {
        html += `<div class="seat-row"><div class="row-label">${hang}</div>`;

        for (let cot = 1; cot <= soCot; cot++) {
            let soGhe = `${hang}${cot}`;
            let ghe = gheTheoSo[soGhe];
            const gridClass = `seat-column-${cot}`;

            if (hang === "F") {
                if (cot % 2 === 0) {
                    continue;
                }

                const soGheDoi = `${hang}${Math.ceil(cot / 2)}`;
                ghe = gheTheoSo[soGheDoi] || ghe;

                if (!ghe) {
                    html += `<div class="seat-space couple-space ${gridClass}"></div>`;
                    continue;
                }

                html += renderGhe(ghe, soGheDoi, `seat couple ${gridClass}`);
                continue;
            }

            if (!ghe) {
                html += `<div class="seat-space ${gridClass}"></div>`;
                continue;
            }

            html += renderGhe(ghe, soGhe, `seat ${layLoaiGhe(hang)} ${gridClass}`);
        }

        html += `</div>`;
    });

    html += `
            </div>
            <div class="seat-legend">
                <div><span class="legend-box standard"></span>Ghế thường</div>
                <div><span class="legend-box vip"></span>Ghế vip</div>
                <div><span class="legend-box couple"></span>Ghế đôi</div>
                <div><span class="legend-box selected"></span>Ghế bạn chọn</div>
                <div><span class="legend-box booked"></span>Ghế đã đặt</div>
            </div>
        </div>
    `;

    return html;
}

function renderGhe(ghe, soGhe, className) {
    const biKhoa = Boolean(ghe.daDat || ghe.dangGiu);
    const finalClass = `${className}${biKhoa ? " booked" : ""}`;
    const disabled = biKhoa ? "disabled" : "";
    const title = ghe.dangGiu ? `${soGhe} - đang được giữ thanh toán` : soGhe;

    return `
        <button
            class="${finalClass}"
            ${disabled}
            onclick="chonGhe(${ghe.maGhe})"
            data-ma-ghe="${ghe.maGhe}"
            title="${title}"
        >
            <span>${soGhe}</span>
        </button>
    `;
}

function layLoaiGhe(hang) {
    if (["C", "D", "E"].includes(hang)) {
        return "vip";
    }

    return "standard";
}

function chonGhe(maGhe) {
    const gheButton = document.querySelector(`[data-ma-ghe="${maGhe}"]`);

    if (!gheButton || gheButton.disabled) {
        return;
    }

    if (gheDangChon.includes(maGhe)) {
        gheDangChon = gheDangChon.filter(item => item !== maGhe);
        gheButton.classList.remove("selected");
    } else {
        gheDangChon.push(maGhe);
        gheButton.classList.add("selected");
    }

    capNhatTamTinhGhe();
}

function capNhatTamTinhGhe() {
    const gia = Array.from(document.querySelectorAll(".seat.selected"))
        .reduce((tongTien, ghe) => tongTien + layGiaGhe(ghe), 0);

    document.getElementById("tamTinhGhe").innerText = formatTien(gia);
}

function layGiaGhe(ghe) {
    if (!ghe) {
        return 0;
    }

    if (ghe.classList.contains("couple")) {
        return 160000;
    }

    if (ghe.classList.contains("vip")) {
        return 90000;
    }

    return 60000;
}

function dongSeatModal() {
    document.getElementById("seatModal").classList.add("hidden");
    document.getElementById("dsGhe").innerHTML = "";
    gioHangDangSuaKey = null;
    gheDangChon = [];
}

function xoaKhoiGioHang(key) {
    luuGioHang(layGioHang().filter(item => item.key !== key));
    renderGioHang();
}

function xoaToanBoGioHang() {
    if (layGioHang().length === 0 || !confirm("Xóa toàn bộ giỏ hàng?")) {
        return;
    }

    luuGioHang([]);
    renderGioHang();
}

function layGioHang() {
    try {
        return JSON.parse(localStorage.getItem(layGioHangKey())) || [];
    } catch (e) {
        localStorage.removeItem(layGioHangKey());
        return [];
    }
}

function luuGioHang(gioHang) {
    localStorage.setItem(layGioHangKey(), JSON.stringify(gioHang));
}

function layGioHangKey() {
    const userKey = user?.maNguoiDung || user?.email || "guest";
    return `gioHang:${userKey}`;
}

function capNhatGioHangBadge(soLuongVe) {
    const badge = document.getElementById("cartBadge");

    if (!badge) {
        return;
    }

    badge.innerText = soLuongVe;
    badge.classList.toggle("hidden", soLuongVe === 0);
}

function xuLyTaiKhoan() {
    if (!user) {
        window.location.href = "login.html";
        return;
    }

    fetch("/nguoidung/logout", {
        method: "POST",
        credentials: "include"
    }).finally(() => {
        localStorage.removeItem("user");
        user = null;
        capNhatTrangThaiDangNhap();
        window.location.href = "/index.html";
    });
}

function moThongTinCaNhan() {
    if (!user) {
        window.location.href = "login.html";
        return;
    }

    window.location.href = "profile.html";
}

function formatNgayGio(value) {
    if (!value) {
        return "";
    }

    return new Date(value).toLocaleString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    });
}

function formatTien(value) {
    return Number(value || 0).toLocaleString("vi-VN") + " đ";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
