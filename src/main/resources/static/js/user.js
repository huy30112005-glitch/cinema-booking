let danhSachPhim = [];
let user = layUserDaLuu();
let phimDangChon = null;
let gheDangChon = [];
let gheRefreshTimer = null;
let bannerIndex = 0;
let bannerTimer = null;
let dangKiemTraDangNhap = true;
let maPhimDangChoDat = layMaPhimCanDatTuUrl()
    || Number(sessionStorage.getItem("pendingBookingMovieId"));

capNhatTrangThaiDangNhap();
kiemTraDangNhap();
hienThiBanner();
hienThiPhim();

function layUserDaLuu() {

    try {
        return JSON.parse(localStorage.getItem("user"));
    } catch (e) {
        localStorage.removeItem("user");
        return null;
    }

}

function capNhatTrangThaiDangNhap() {

    if (document.getElementById("loiChao")) {
        document.getElementById("loiChao").innerText =
            user ? `Xin chào, ${user.tenNguoiDung}` : "";
    }

    if (document.getElementById("authButton")) {
        document.getElementById("authButton").innerText =
            user ? "Đăng xuất" : "Đăng nhập";
    }

    if (document.getElementById("profileButton")) {
        document.getElementById("profileButton").classList.toggle("hidden", !user);
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
                dangKiemTraDangNhap = false;
                thuMoDatVeDangCho();
                return;
            }

            user = await res.json();
            localStorage.setItem("user", JSON.stringify(user));
            capNhatTrangThaiDangNhap();
            xoaTrangThaiVuaDangNhap();
            dangKiemTraDangNhap = false;
            thuMoDatVeDangCho();

        })
        .catch(() => {
            if (!user) {
                capNhatTrangThaiDangNhap();
            }
            dangKiemTraDangNhap = false;
            thuMoDatVeDangCho();
        });

}

function xoaTrangThaiVuaDangNhap() {

    if (window.location.search.includes("login=success")) {
        window.history.replaceState({}, document.title, "/index.html");
    }

}

function hienThiPhim() {

    fetch("/phim")
        .then(res => res.json())
        .then(data => {

            danhSachPhim = data;
            renderPhim(danhSachPhim);
            thuMoDatVeDangCho();

        });

}

function hienThiBanner() {

    fetch("/banner")
        .then(res => res.json())
        .then(data => {
            renderBanner(data);
        })
        .catch(err => {
            console.log(err);
            renderBanner([]);
        });

}

function renderBanner(data) {

    const banner = document.getElementById("bannerPhim");

    if (!banner) {
        return;
    }

    const danhSachBanner = data.filter(item => taoBannerUrl(item.linkDen));

    if (danhSachBanner.length === 0) {
        banner.classList.add("hidden");
        banner.innerHTML = "";
        return;
    }

    if (bannerIndex >= danhSachBanner.length) {
        bannerIndex = 0;
    }

    const bannerDangChon = danhSachBanner[bannerIndex];
    const bannerUrl = taoBannerUrl(bannerDangChon.linkDen);

    banner.innerHTML = `
        <div class="banner-bg" style="background-image:url('${bannerUrl}')"></div>
        <div class="banner-content">
            <img class="banner-image" src="${bannerUrl}" alt="Banner ${bannerDangChon.maBanner || ""}">
        </div>
        ${danhSachBanner.length > 1 ? `
            <button class="banner-arrow banner-arrow-left" onclick="doiBanner(-1, ${danhSachBanner.length})" aria-label="Banner trước">
                ‹
            </button>
            <button class="banner-arrow banner-arrow-right" onclick="doiBanner(1, ${danhSachBanner.length})" aria-label="Banner tiếp theo">
                ›
            </button>
        ` : ""}
        <div class="banner-dots">
            ${danhSachBanner.map((_, index) => `
                <button
                    class="${index === bannerIndex ? "active" : ""}"
                    onclick="chonBanner(${index})"
                    aria-label="Chọn banner ${index + 1}"
                ></button>
            `).join("")}
        </div>
    `;

    banner.classList.remove("hidden");
    batDauTuDongDoiBanner(danhSachBanner.length);

}

function doiBanner(huong, total) {

    bannerIndex = (bannerIndex + huong + total) % total;
    hienThiBanner();

}

function chonBanner(index) {

    bannerIndex = index;
    hienThiBanner();

}

function batDauTuDongDoiBanner(total) {

    if (bannerTimer) {
        clearInterval(bannerTimer);
    }

    if (total <= 1) {
        return;
    }

    bannerTimer = setInterval(() => {
        bannerIndex = (bannerIndex + 1) % total;
        hienThiBanner();
    }, 6000);

}

function taoBannerUrl(path) {

    return taoPosterUrl(path);

}

function renderPhim(data) {

    let html = "";

    data.forEach(p => {

        html += `
            <div class="movie">

                ${renderPoster(p)}

                <h3>${p.tenPhim}</h3>

                <p>
                    <b>Thời lượng:</b>
                    ${p.thoiLuong} phút
                </p>

                <p>
                    <b>Mô tả:</b>
                    ${p.moTa}
                </p>

                <p>
                    <b>Định dạng:</b>
                    ${p.maDinhDang?.tenDinhDang || ""}
                </p>

                <p>
                    <b>Thể loại:</b>
                    ${p.maTheLoai?.tenTheLoai || ""}
                </p>

                <p>
                    <b>Độ tuổi:</b>
                    ${p.doTuoi || "Chưa cập nhật"}
                </p>

                <button
                    class="btn-them"
                    onclick="moTrangChiTietPhim(${p.maPhim})"
                >
                    Xem chi tiết
                </button>

            </div>
        `;
    });

    document.getElementById("dsPhim").innerHTML = html;

}

function moDatVeTuTrangChiTietNeuCo() {

    maPhimDangChoDat = layMaPhimCanDatTuUrl()
        || Number(sessionStorage.getItem("pendingBookingMovieId"))
        || maPhimDangChoDat;
    thuMoDatVeDangCho();

}

function layMaPhimCanDatTuUrl() {

    return Number(new URLSearchParams(window.location.search).get("datve"));

}

function thuMoDatVeDangCho() {

    if (!maPhimDangChoDat || danhSachPhim.length === 0) {
        return;
    }

    if (dangKiemTraDangNhap && !user) {
        return;
    }

    if (!user) {
        sessionStorage.setItem("pendingBookingMovieId", maPhimDangChoDat);
        window.location.href = "login.html";
        return;
    }

    const maPhim = maPhimDangChoDat;
    maPhimDangChoDat = null;
    sessionStorage.removeItem("pendingBookingMovieId");
    window.history.replaceState({}, document.title, "/index.html#datVePanel");
    chonPhim(maPhim);

}

function moTrangChiTietPhim(maPhim) {

    window.location.href = `movie-detail.html?id=${maPhim}`;

}

function renderPoster(phim) {

    const posterUrl = taoPosterUrl(phim.anhPoster);

    if (!posterUrl) {
        return "";
    }

    return `
        <img
            class="movie-poster"
            src="${posterUrl}"
            alt="Poster ${phim.tenPhim || ""}"
        >
    `;

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

function renderTrailerButton(phim) {

    const embedUrl = taoTrailerEmbedUrl(phim.trailer);

    if (!embedUrl) {
        return "";
    }

    return `
        <button
            class="btn-sua trailer-toggle"
            onclick="moTrailerModal('${embedUrl}')"
        >
            Xem trailer
        </button>
    `;

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

function layYoutubeId(value) {

    if (/^[A-Za-z0-9_-]{11}$/.test(value)) {
        return value;
    }

    const match = value.match(/(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|shorts\/))([A-Za-z0-9_-]{11})/);
    return match ? match[1] : "";

}

function locPhim() {

    let tuKhoa = document.getElementById("tuKhoaPhim").value.toLowerCase();

    let ketQua = danhSachPhim.filter(p => {

        let noiDung = `
            ${p.tenPhim || ""}
            ${p.moTa || ""}
            ${p.maDinhDang?.tenDinhDang || ""}
            ${p.maTheLoai?.tenTheLoai || ""}
        `.toLowerCase();

        return noiDung.includes(tuKhoa);

    });

    renderPhim(ketQua);

}

function chonPhim(maPhim) {

    if (!user) {
        alert("Vui lòng đăng nhập để đặt vé");
        window.location.href = "login.html";
        return;
    }

    phimDangChon = danhSachPhim.find(p => p.maPhim === maPhim);
    gheDangChon = [];

    if (!phimDangChon) {
        alert("Không tìm thấy phim");
        return;
    }

    document.getElementById("tenPhimDangChon").innerText =
        `Đặt vé: ${phimDangChon.tenPhim}`;

    document.getElementById("thongTinDatVe").innerText =
        "Chọn suất chiếu và ghế bạn muốn đặt";

    document.getElementById("datVePanel").classList.remove("hidden");
    document.getElementById("dsGhe").innerHTML = "";
    document.getElementById("maSuatChieu").value = "";
    document.getElementById("datVePanel").scrollIntoView({ behavior: "smooth", block: "start" });

    loadSuatChieu(maPhim);

}

function loadSuatChieu(maPhim) {

    fetch(`/datve/suatchieu/${maPhim}`)
        .then(res => res.json())
        .then(data => {

            document.getElementById("lichChieuDatVe").innerHTML =
                renderLichChieu(data, "booking");

            if (data.length === 0) {
                document.getElementById("thongTinDatVe").innerText =
                    "Phim này chưa có suất chiếu";
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
                                onclick="chonSuatChieu(${sc.maSuatChieu})"
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

function layNgayKey(date) {

    const nam = date.getFullYear();
    const thang = String(date.getMonth() + 1).padStart(2, "0");
    const ngay = String(date.getDate()).padStart(2, "0");

    return `${nam}-${thang}-${ngay}`;

}

function chonNgayLichChieu(prefix, key) {

    const root = document.getElementById(
        prefix === "booking" ? "lichChieuDatVe" : "lichChieuChiTiet"
    );

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

function chonSuatChieu(maSuatChieu) {

    document.getElementById("maSuatChieu").value = maSuatChieu;

    document.querySelectorAll(".showtime-chip").forEach(btn => {
        btn.classList.toggle(
            "active",
            Number(btn.dataset.showtimeId) === Number(maSuatChieu)
        );
    });

    loadGheTheoSuatChieu();

}

function formatKhoangGio(batDau, ketThuc) {

    const gioBatDau = formatGio(batDau);
    const gioKetThuc = ketThuc ? formatGio(ketThuc) : "";

    return gioKetThuc ? `${gioBatDau} ~ ${gioKetThuc}` : gioBatDau;

}

function formatGio(value) {

    return new Date(value).toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit"
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

            document.getElementById("dsGhe").innerHTML =
                renderSoDoGhe(data);

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

        html += `
            <div class="seat-row">
                <div class="row-label">${hang}</div>
        `;

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

        html += `
            </div>
        `;
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

    if (cot >= 3 && cot <= 10 && ["B", "C", "D"].includes(hang)) {
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

    fetch("/payment/create", {

        method: "POST",
        credentials: "include",

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify({
            maSuatChieu: parseInt(maSuatChieu),
            maGheList: gheDangChon
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

                // backend có thể trả {success:false,message:"..."}
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

function dongDatVe() {

    phimDangChon = null;
    gheDangChon = [];
    dongSeatModal();
    dungTuDongTaiGhe();
    document.getElementById("datVePanel").classList.add("hidden");

}

function dungTuDongTaiGhe() {

    if (gheRefreshTimer) {
        clearInterval(gheRefreshTimer);
        gheRefreshTimer = null;
    }

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

function xuLyTaiKhoan() {

    if (!user) {
        window.location.href = "login.html";
        return;
    }

    dangXuat();

}

function moThongTinCaNhan() {

    if (!user) {
        window.location.href = "login.html";
        return;
    }

    window.location.href = "profile.html";

}

function dangXuat() {

    localStorage.removeItem("user");

    fetch("/nguoidung/logout", {
        method: "POST",
        credentials: "include"
    }).finally(() => {
        window.location.href = "/index.html";
    });

}
