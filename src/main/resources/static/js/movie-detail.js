let phimDangXem = null;
let user = layUserDaLuu();
let gheDangChon = [];
let gheRefreshTimer = null;
let danhSachSuatChieuChiTiet = [];
let gheCanChonSauTai = [];
let dangSuaGioHangKey = null;

capNhatTrangThaiDangNhap();
kiemTraDangNhap();
loadChiTietPhim();
capNhatGioHangBadge();

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
                return;
            }

            user = await res.json();
            localStorage.setItem("user", JSON.stringify(user));
            capNhatTrangThaiDangNhap();
            capNhatGioHangBadge();

        })
        .catch(() => {
            if (!user) {
                capNhatTrangThaiDangNhap();
            }
        });

}

function loadChiTietPhim() {

    const maPhim = Number(new URLSearchParams(window.location.search).get("id"));
    const container = document.getElementById("movieDetail");

    if (!maPhim) {
        container.innerHTML = `<p class="muted">Không tìm thấy mã phim.</p>`;
        return;
    }

    fetch("/phim")
        .then(res => res.json())
        .then(data => {

            phimDangXem = data.find(p => p.maPhim === maPhim);

            if (!phimDangXem) {
                container.innerHTML = `<p class="muted">Không tìm thấy phim.</p>`;
                return;
            }

            renderChiTietPhim(phimDangXem);
            loadLichChieuChiTiet(maPhim);

        })
        .catch(err => {
            console.log(err);
            container.innerHTML = `<p class="muted">Không tải được chi tiết phim.</p>`;
        });

}

function renderChiTietPhim(phim) {

    const container = document.getElementById("movieDetail");
    const breadcrumbTitle = document.getElementById("breadcrumbTitle");
    const posterUrl = taoPosterUrl(phim.anhPoster);
    const embedUrl = taoTrailerEmbedUrl(phim.trailer);
    const doTuoi = phim.doTuoi || "P";

    document.title = `${phim.tenPhim || "Chi tiết phim"} - CKB Cinema`;
    breadcrumbTitle.innerText = phim.tenPhim || "Chi tiết phim";

    container.innerHTML = `
        <div class="movie-detail-layout">
            <div class="movie-detail-poster">
                ${posterUrl
                    ? `<img src="${posterUrl}" alt="Poster ${phim.tenPhim || ""}">`
                    : `<div class="poster-placeholder">Chưa có poster</div>`}
            </div>

            <div class="movie-detail-content">
                <div class="movie-rating-line">
                    <span class="age-badge ${layClassDoTuoi(doTuoi)}">${doTuoi}</span>
                    <span>Đang chiếu</span>
                </div>

                <h2>${phim.tenPhim || "Chi tiết phim"}</h2>

                <div class="movie-detail-actions">
                    <button class="btn-them" onclick="datVeTuTrangChiTiet()">
                        Đặt vé ngay
                    </button>
                    ${embedUrl ? `
                        <button class="btn-sua trailer-toggle" onclick="moTrailerModal('${embedUrl}')">
                            Xem trailer
                        </button>
                    ` : ""}
                </div>

                <div class="movie-description-block">
                    <h3>Nội dung</h3>
                    <p>${phim.moTa || "Chưa cập nhật nội dung phim."}</p>
                </div>

                <div class="movie-detail-meta">
                    <div>
                        <span>Thời lượng</span>
                        <p>${phim.thoiLuong || "Chưa cập nhật"} phút</p>
                    </div>
                    <div>
                        <span>Thể loại</span>
                        <p>${phim.maTheLoai?.tenTheLoai || "Chưa cập nhật"}</p>
                    </div>
                </div>
            </div>
        </div>

        <div class="detail-showtime-section">
            <h3>Lịch chiếu của phim <span>${phim.tenPhim || ""}</span></h3>
            <div id="lichChieuChiTiet" class="detail-showtime-list">
                Đang tải lịch chiếu...
            </div>
        </div>
    `;

}

function layClassDoTuoi(doTuoi) {

    const normalized = String(doTuoi || "P").trim().toUpperCase();

    if (["P", "K", "5+", "7+"].includes(normalized)) {
        return "age-green";
    }

    if (["13+", "T13", "C13"].includes(normalized)) {
        return "age-yellow";
    }

    if (["16+", "T16", "C16"].includes(normalized)) {
        return "age-orange";
    }

    if (["18+", "T18", "C18"].includes(normalized)) {
        return "age-red";
    }

    return "age-gray";

}

function loadLichChieuChiTiet(maPhim) {

    fetch(`/datve/suatchieu/${maPhim}`)
        .then(res => res.json())
        .then(data => {

            const container = document.getElementById("lichChieuChiTiet");
            danhSachSuatChieuChiTiet = data;

            if (!container) {
                return;
            }

            if (data.length === 0) {
                container.innerHTML = `<p class="muted">Phim này chưa có suất chiếu.</p>`;
                return;
            }

            container.innerHTML = renderLichChieu(data, "detail");

        })
        .catch(() => {

            const container = document.getElementById("lichChieuChiTiet");

            if (container) {
                container.innerHTML = `<p class="muted">Không tải được lịch chiếu.</p>`;
            }

        });

}

function datVeTuTrangChiTiet() {

    if (!phimDangXem) {
        return;
    }

    if (!user) {
        alert("Vui lòng đăng nhập để đặt vé");
        window.location.href = "login.html";
        return;
    }

    document
        .getElementById("lichChieuChiTiet")
        ?.scrollIntoView({ behavior:"smooth", block:"center" });

}

function loadSuatChieu(maPhim) {

    fetch(`/datve/suatchieu/${maPhim}`)
        .then(res => res.json())
        .then(data => {

            danhSachSuatChieuChiTiet = data;

            if (data.length === 0) {
                const container = document.getElementById("lichChieuChiTiet");

                if (container) {
                    container.innerHTML = `<p class="showtime-empty">Phim này chưa có suất chiếu.</p>`;
                }
            }

        })
        .catch(err => {
            console.log(err);
            alert("Không tải được suất chiếu");
        });

}

function renderLichChieu(data, prefix) {

    if (!data || data.length === 0) {
        return `<p class="showtime-empty">Phim này chưa có suất chiếu.</p>`;
    }

    const theoNgay = gomSuatChieuTheoNgay(data);

    return `
        <div class="showtime-days">
            ${theoNgay.map((nhom, index) => `
                <button
                    type="button"
                    class="showtime-day ${index === 0 ? "active" : ""}"
                    data-showtime-day="${nhom.key}"
                    onclick="chonNgayLichChieu('${prefix}', '${nhom.key}')"
                >
                    <strong>${nhom.day}</strong>
                    <span>${nhom.label}</span>
                </button>
            `).join("")}
        </div>

        ${theoNgay.map((nhom, index) => `
            <div
                class="showtime-day-panel ${index === 0 ? "" : "hidden"}"
                data-showtime-panel="${nhom.key}"
            >
                <div class="showtime-format">
                    <h4>2D Phụ đề</h4>
                    <div class="showtime-times">
                        ${nhom.items.map(sc => `
                            <button
                                type="button"
                                class="showtime-chip"
                                data-showtime-id="${sc.maSuatChieu}"
                                onclick="${prefix === "detail"
                    ? `chonSuatChieuChiTiet(${sc.maSuatChieu})`
                    : `chonSuatChieu(${sc.maSuatChieu})`}"
                            >
                                ${formatKhoangGio(sc.thoiGianBatDau, sc.thoiGianKetThuc)}
                            </button>
                        `).join("")}
                    </div>
                </div>
            </div>
        `).join("")}
    `;

}

function gomSuatChieuTheoNgay(data) {

    const map = new Map();

    data.forEach(sc => {
        const ngay = new Date(sc.thoiGianBatDau);
        const key = layNgayKey(ngay);

        if (!map.has(key)) {
            map.set(key, {
                key,
                day: String(ngay.getDate()).padStart(2, "0"),
                label: layNhanNgay(ngay),
                items: []
            });
        }

        map.get(key).items.push(sc);
    });

    return Array.from(map.values())
        .sort((a, b) => a.key.localeCompare(b.key))
        .map(nhom => ({
            ...nhom,
            items: nhom.items.sort((a, b) =>
                new Date(a.thoiGianBatDau) - new Date(b.thoiGianBatDau))
        }));

}

function layNgayKey(date) {

    const nam = date.getFullYear();
    const thang = String(date.getMonth() + 1).padStart(2, "0");
    const ngay = String(date.getDate()).padStart(2, "0");

    return `${nam}-${thang}-${ngay}`;

}

function layNhanNgay(date) {

    if (layNgayKey(new Date()) === layNgayKey(date)) {
        return "Hôm nay";
    }

    const thu = [
        "Chủ nhật",
        "Thứ 2",
        "Thứ 3",
        "Thứ 4",
        "Thứ 5",
        "Thứ 6",
        "Thứ 7"
    ];

    return thu[date.getDay()];

}

function chonNgayLichChieu(prefix, key) {

    const root = document.getElementById("lichChieuChiTiet");

    if (!root) {
        return;
    }

    root.querySelectorAll(".showtime-day").forEach(btn => {
        btn.classList.toggle("active", btn.dataset.showtimeDay === key);
    });

    root.querySelectorAll(".showtime-day-panel").forEach(panel => {
        panel.classList.toggle("hidden", panel.dataset.showtimePanel !== key);
    });

}

function chonSuatChieuChiTiet(maSuatChieu) {

    if (!user) {
        alert("Vui lòng đăng nhập để đặt vé");
        window.location.href = "login.html";
        return;
    }

    document.getElementById("dsGhe").innerHTML = "";
    chonSuatChieu(maSuatChieu);

}

function chonSuatChieu(maSuatChieu) {

    document.getElementById("maSuatChieu").value = maSuatChieu;

    document.querySelectorAll(".showtime-chip").forEach(btn => {
        btn.classList.toggle(
            "active",
            Number(btn.dataset.showtimeId) === Number(maSuatChieu)
        );
    });

    const sc = danhSachSuatChieuChiTiet.find(item =>
        Number(item.maSuatChieu) === Number(maSuatChieu)
    );

    if (sc) {
        const key = layNgayKey(new Date(sc.thoiGianBatDau));
        chonNgayLichChieu("detail", key);
    }

    loadGheTheoSuatChieu();

}

function formatKhoangGio(batDau, ketThuc) {

    const gioBatDau = formatGio(batDau);
    const gioKetThuc = ketThuc ? formatGio(ketThuc) : "";

    return gioKetThuc ? `${gioBatDau} ~ ${gioKetThuc}` : gioBatDau;

}

function formatGio(value) {

    return new Date(value).toLocaleTimeString("vi-VN", {
        hour:"2-digit",
        minute:"2-digit"
    });

}

function loadGheTheoSuatChieu(silent = false) {

    let maSuatChieu = document.getElementById("maSuatChieu").value;
    if (!silent) {
        gheDangChon = [];
    }
    dungTuDongTaiGhe();

    if (!maSuatChieu) {
        document.getElementById("dsGhe").innerHTML = "";
        dongSeatModal();
        return;
    }

    fetch(`/datve/ghe/${maSuatChieu}`)
        .then(async res => {

            if (!res.ok) {
                let message = await res.text();
                throw new Error(message || "Không tải được ghế");
            }

            return res.json();

        })
        .then(data => {
            document.getElementById("dsGhe").innerHTML = renderSoDoGhe(data);

            if (gheCanChonSauTai.length > 0) {
                gheDangChon = [...gheCanChonSauTai];
                gheCanChonSauTai = [];
            }

            gheDangChon = gheDangChon.filter(maGhe => {
                const gheButton = document.querySelector(`[data-ma-ghe="${maGhe}"]`);

                if (gheButton && !gheButton.disabled) {
                    gheButton.classList.add("selected");
                    return true;
                } else {
                    return false;
                }
            });

            capNhatTamTinhGhe();

            if (!silent) {
                moSeatModal();
            }
        })
        .catch(err => {

            console.log(err);
            if (!silent) {
                alert(err.message || "Không tải được danh sách ghế");
            }

        });

    gheRefreshTimer = setInterval(() => {
        loadGheTheoSuatChieu(true);
    }, 15000);

}

function renderSoDoGhe(data) {

    if (data.length === 0) {
        return `<p class="muted">Phòng này chưa có ghế</p>`;
    }

    let gheTheoSo = {};

    data.forEach(ghe => {
        gheTheoSo[ghe.soGhe] = ghe;
    });

    let hangGhe = ["A", "B", "C", "D", "E", "F"];
    let soCot = 12;

    let html = `
        <div class="seat-map">
            <div class="screen-line"></div>
            <div class="screen-title">Màn hình chính</div>
            <div class="column-labels">
                <div></div>
                ${Array.from({ length: soCot }, (_, i) => {
                    const cot = i + 1;
                    return `<span class="seat-column seat-column-${cot}">${cot}</span>`;
                }).join("")}
            </div>
            <div class="seat-map-body">
    `;

    hangGhe.forEach(hang => {

        html += `<div class="seat-row"><div class="row-label">${hang}</div>`;

        for (let cot = 1; cot <= soCot; cot++) {

            let soGhe = `${hang}${cot}`;
            let ghe = gheTheoSo[soGhe];
            let gridClass = `seat-column-${cot}`;

            if (hang === "F") {
                if (cot % 2 === 0) {
                    continue;
                }

                const chiSoGheDoi = Math.ceil(cot / 2);
                const soGheDoi = `${hang}${chiSoGheDoi}`;
                ghe = gheTheoSo[soGheDoi] || ghe;

                if (!ghe) {
                    html += `<div class="seat-space couple-space ${gridClass}"></div>`;
                    continue;
                }

                let className = "seat couple";
                const biKhoa = Boolean(ghe.daDat || ghe.dangGiu);

                if (biKhoa) {
                    className += " booked";
                }

                let disabled = biKhoa ? "disabled" : "";
                let title = soGheDoi;

                html += `
                    <button
                        class="${className} ${gridClass}"
                        ${disabled}
                        onclick="chonGhe(${ghe.maGhe})"
                        data-ma-ghe="${ghe.maGhe}"
                        title="${title}"
                    >
                        <span>${title}</span>
                    </button>
                `;
                continue;
            }

            if (!ghe) {
                html += `<div class="seat-space ${gridClass}"></div>`;
                continue;
            }

            let loaiGhe = layLoaiGhe(hang, cot);
            let className = `seat ${loaiGhe}`;

            if (ghe.daDat || ghe.dangGiu) {
                className += " booked";
            }

            let disabled = (ghe.daDat || ghe.dangGiu) ? "disabled" : "";
            let title = ghe.dangGiu ? `${soGhe} - đang được giữ thanh toán` : soGhe;

            html += `
                <button
                    class="${className} ${gridClass}"
                    ${disabled}
                    onclick="chonGhe(${ghe.maGhe})"
                    data-ma-ghe="${ghe.maGhe}"
                    title="${title}"
                >
                    <span>${soGhe}</span>
                </button>
            `;
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
                <div><span class="legend-box center-zone-box"></span>Vùng trung tâm</div>
            </div>
        </div>
    `;

    return html;

}

function layLoaiGhe(hang, cot) {

    if (hang === "F") {
        return "couple";
    }

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

function moSeatModal() {

    const modal = document.getElementById("seatModal");

    if (modal) {
        modal.classList.remove("hidden");
    }

}

function dongSeatModal() {

    const modal = document.getElementById("seatModal");

    if (modal) {
        modal.classList.add("hidden");
    }

    gheDangChon = [];
    document.querySelectorAll(".seat").forEach(btn => {
        btn.classList.remove("selected");
    });
    capNhatTamTinhGhe();
    dungTuDongTaiGhe();

}

function capNhatTamTinhGhe() {

    const tamTinh = document.getElementById("tamTinhGhe");

    if (!tamTinh) {
        return;
    }

    const gia = Array.from(document.querySelectorAll(".seat.selected"))
        .reduce((tongTien, ghe) => tongTien + layGiaGhe(ghe), 0);

    tamTinh.innerText = gia.toLocaleString("vi-VN") + " đ";

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

function xacNhanDatVe() {

    if (!user) {
        alert("Vui lòng đăng nhập để đặt vé");
        window.location.href = "login.html";
        return;
    }

    let maSuatChieu = document.getElementById("maSuatChieu").value;

    if (!maSuatChieu) {
        alert("Vui lòng chọn suất chiếu");
        return;
    }

    if (gheDangChon.length === 0) {
        alert("Vui lòng chọn ghế");
        return;
    }

    taoThanhToan(parseInt(maSuatChieu), gheDangChon);

}

function taoThanhToan(maSuatChieu, maGheList, gioHangKey = null) {

    fetch("/payment/create", {
        method:"POST",
        credentials:"include",
        headers:{
            "Content-Type":"application/json"
        },
        body:JSON.stringify({
            maSuatChieu:maSuatChieu,
            maGhe:maGheList[0],
            maGheList:maGheList
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

                const ct = res.headers.get("content-type") || "";
                if (ct.includes("application/json")) {
                    const data = await res.json();
                    throw new Error(data.message || "Không tạo được thanh toán");
                }

                let message = await res.text();
                throw new Error(message);
            }

            return res.json();

        })
        .then(data => {
            if (gioHangKey) {
                luuGioHang(layGioHang().filter(item => item.key !== gioHangKey));
            }
            window.location.href = data.paymentUrl;
        })
        .catch(err => {

            if (err.message === "REDIRECT_LOGIN") {
                return;
            }

            console.log(err);
            alert(err.message || "Không tạo được thanh toán");

        });

}

function themGheVaoGioHang() {

    if (!user) {
        alert("Vui lòng đăng nhập để thêm vé vào giỏ hàng");
        window.location.href = "login.html";
        return;
    }

    const maSuatChieu = Number(document.getElementById("maSuatChieu").value);

    if (!maSuatChieu || gheDangChon.length === 0) {
        alert("Vui lòng chọn suất chiếu và ghế");
        return;
    }

    const suatChieu = danhSachSuatChieuChiTiet.find(item =>
        Number(item.maSuatChieu) === maSuatChieu
    );

    const gheList = gheDangChon.map(maGhe => {
        const gheButton = document.querySelector(`[data-ma-ghe="${maGhe}"]`);
        return {
            maGhe,
            soGhe:gheButton?.getAttribute("title")?.split(" - ")[0] || String(maGhe),
            gia:layGiaGhe(gheButton)
        };
    });

    const gioHang = layGioHang();
    const key = taoGioHangItemKey(maSuatChieu);
    const itemDangSua = gioHang.find(cartItem => cartItem.key === key);
    const item = {
        key,
        maPhim:itemDangSua?.maPhim || phimDangXem?.maPhim || null,
        tenPhim:itemDangSua?.tenPhim || phimDangXem?.tenPhim || "Phim",
        maSuatChieu,
        thoiGianBatDau:suatChieu?.thoiGianBatDau || itemDangSua?.thoiGianBatDau || null,
        thoiGianKetThuc:suatChieu?.thoiGianKetThuc || itemDangSua?.thoiGianKetThuc || null,
        tenPhong:suatChieu?.tenPhong || itemDangSua?.tenPhong || "",
        gheList,
        maGheList:gheList.map(ghe => ghe.maGhe),
        tongTien:gheList.reduce((tong, ghe) => tong + ghe.gia, 0)
    };

    const viTriCu = gioHang.findIndex(cartItem => cartItem.key === key);

    if (viTriCu >= 0) {
        gioHang[viTriCu] = item;
    } else {
        gioHang.push(item);
    }

    luuGioHang(gioHang);
    dangSuaGioHangKey = null;
    dongSeatModal();
    window.location.href = "/cart.html";

}

function moGioHang() {

    renderGioHang();
    document.getElementById("gioHangPanel")?.classList.remove("hidden");

}

function dongGioHang() {

    document.getElementById("gioHangPanel")?.classList.add("hidden");

}

function renderGioHang() {

    const container = document.getElementById("gioHangList");

    if (!container) {
        return;
    }

    const gioHang = layGioHang();
    capNhatGioHangBadge();

    if (gioHang.length === 0) {
        container.innerHTML = `<p class="muted">Giỏ hàng chưa có vé.</p>`;
        return;
    }

    container.innerHTML = gioHang.map(item => `
        <article class="cart-item">
            <div>
                <h3>${item.tenPhim}</h3>
                <p>${formatNgayGio(item.thoiGianBatDau)}${item.tenPhong ? ` - ${item.tenPhong}` : ""}</p>
                <p>Ghế: <strong>${(item.gheList || []).map(ghe => ghe.soGhe).join(", ")}</strong></p>
                <p class="cart-price">${Number(item.tongTien || 0).toLocaleString("vi-VN")} đ</p>
            </div>
            <div class="cart-actions">
                <button class="btn-sua" onclick="doiGheTrongGioHang('${item.key}')">Đổi ghế</button>
                <button class="btn-them" onclick="thanhToanGioHang('${item.key}')">Thanh toán</button>
                <button class="btn-xoa" onclick="xoaKhoiGioHang('${item.key}')">Xóa</button>
            </div>
        </article>
    `).join("");

}

function doiGheTrongGioHang(key) {

    const item = layGioHang().find(cartItem => cartItem.key === key);

    if (!item) {
        return;
    }

    dangSuaGioHangKey = key;
    gheCanChonSauTai = item.maGheList || [];
    document.getElementById("maSuatChieu").value = item.maSuatChieu;
    chonSuatChieu(item.maSuatChieu);

}

function thanhToanGioHang(key) {

    const item = layGioHang().find(cartItem => cartItem.key === key);

    if (!item) {
        return;
    }

    taoThanhToan(item.maSuatChieu, item.maGheList, key);

}

function xoaKhoiGioHang(key) {

    luuGioHang(layGioHang().filter(item => item.key !== key));
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
    capNhatGioHangBadge();

}

function layGioHangKey() {

    const userKey = user?.maNguoiDung || user?.email || "guest";
    return `gioHang:${userKey}`;

}

function taoGioHangItemKey(maSuatChieu) {

    return `suat-${maSuatChieu}`;

}

function capNhatGioHangBadge() {

    const badge = document.getElementById("cartBadge");

    if (!badge) {
        return;
    }

    const soLuongVe = layGioHang()
        .reduce((tong, item) => tong + (item.maGheList?.length || 0), 0);

    badge.innerText = soLuongVe;
    badge.classList.toggle("hidden", soLuongVe === 0);

}

function dongDatVe() {

    gheDangChon = [];
    dongSeatModal();
    dungTuDongTaiGhe();

}

function dungTuDongTaiGhe() {

    if (gheRefreshTimer) {
        clearInterval(gheRefreshTimer);
        gheRefreshTimer = null;
    }

}

function taoPosterUrl(path) {

    if (!path) {
        return "";
    }

    const normalized = path.trim().replaceAll("\\", "/");

    if (normalized.startsWith("http://")
            || normalized.startsWith("https://")
            || normalized.startsWith("/")) {
        return normalized;
    }

    return `/${normalized}`;

}

function taoTrailerEmbedUrl(trailer) {

    if (!trailer) {
        return "";
    }

    const value = trailer.trim();
    const youtubeId = layYoutubeId(value);

    if (youtubeId) {
        return `https://www.youtube.com/embed/${youtubeId}`;
    }

    if (value.includes("youtube.com/embed/")) {
        return value;
    }

    return "";

}

function layYoutubeId(value) {

    if (/^[A-Za-z0-9_-]{11}$/.test(value)) {
        return value;
    }

    const match = value.match(/(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|shorts\/))([A-Za-z0-9_-]{11})/);
    return match ? match[1] : "";

}

function moTrailerModal(embedUrl) {

    let modal = document.getElementById("trailerModal");

    if (!modal) {
        document.body.insertAdjacentHTML("beforeend", `
            <div id="trailerModal" class="trailer-modal hidden">
                <div class="trailer-modal-content">
                    <button class="trailer-close" onclick="dongTrailerModal()">Đóng</button>
                    <iframe
                        id="trailerModalFrame"
                        src=""
                        title="Trailer phim"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowfullscreen
                    ></iframe>
                </div>
            </div>
        `);
        modal = document.getElementById("trailerModal");
    }

    document.getElementById("trailerModalFrame").src = embedUrl;
    modal.classList.remove("hidden");

}

function dongTrailerModal() {

    const modal = document.getElementById("trailerModal");
    const frame = document.getElementById("trailerModalFrame");

    if (frame) {
        frame.src = "";
    }

    if (modal) {
        modal.classList.add("hidden");
    }

}

function formatNgayGio(value) {

    if (!value) {
        return "";
    }

    return new Date(value).toLocaleString("vi-VN", {
        hour:"2-digit",
        minute:"2-digit",
        day:"2-digit",
        month:"2-digit",
        year:"numeric"
    });

}

function xuLyTaiKhoan() {

    if (!user) {
        window.location.href = "login.html";
        return;
    }

    fetch("/nguoidung/logout", {
        method:"POST",
        credentials:"include"
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
