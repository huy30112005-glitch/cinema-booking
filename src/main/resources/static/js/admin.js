let danhSachPhim = [];
let danhSachPhong = [];
let danhSachSuatChieu = [];
let danhSachBanner = [];
let user = JSON.parse(localStorage.getItem("user"));
let qrScannerStream = null;
let qrScannerTimer = null;
let qrDetector = null;
let qrCanvas = null;
let qrCanvasContext = null;

if (user == null) {
    window.location.href = "login.html";
}

if (!laAdmin(user)) {
    window.location.href = "index.html";
}

hienThiPhim();
loadDinhDang();
loadTheLoai();
loadPhong();
loadSuatChieu();
loadThongTinChuyenKhoan();
loadBanner();
hienThiMucAdmin("ticket", false);

function laAdmin(user) {
    return (user?.vaiTro || "").toUpperCase() === "ADMIN";
}

function hienThiMucAdmin(section, scrollToSection = true) {

    document.querySelectorAll(".admin-section").forEach(item => {
        item.classList.toggle("hidden", item.dataset.adminSection !== section);
    });

    document.querySelectorAll(".admin-nav-link").forEach(link => {
        link.classList.toggle(
            "active",
            link.getAttribute("onclick")?.includes(`'${section}'`)
        );
    });

    if (scrollToSection) {
        document
            .querySelector(`.admin-section[data-admin-section="${section}"]`)
            ?.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
    }

}

function loadBanner() {

    fetch("http://localhost:8081/banner")
        .then(res => res.json())
        .then(data => {
            danhSachBanner = data;
            renderBannerAdmin(danhSachBanner);
        })
        .catch(err => {
            console.log(err);
            document.getElementById("dsBanner").innerHTML =
                `<p class="muted">Không tải được danh sách banner</p>`;
        });

}

function renderBannerAdmin(data) {

    let html = "";

    data.forEach(banner => {
        const bannerUrl = taoPosterUrl(banner.linkDen);

        html += `
            <div class="movie banner-admin-item">
                ${bannerUrl ? `<img class="banner-admin-preview" src="${bannerUrl}" alt="Banner ${banner.maBanner}">` : ""}

                <p>
                    <b>Link:</b>
                    ${banner.linkDen || ""}
                </p>

                <p>
                    <b>Thứ tự:</b>
                    ${banner.thuTu ?? ""}
                </p>

                <button
                    class="btn-xoa"
                    onclick="xoaBanner(${banner.maBanner})"
                >
                    Xóa banner
                </button>
            </div>
        `;
    });

    document.getElementById("dsBanner").innerHTML =
        html || `<p class="muted">Chưa có banner</p>`;

}

function themBanner() {

    const image = document.getElementById("bannerImage").files[0];
    const formData = new FormData();
    const thuTu = document.getElementById("bannerThuTu").value;

    if (!image) {
        alert("Vui lòng chọn ảnh banner");
        return;
    }

    formData.append("image", image);

    if (thuTu) {
        formData.append("thuTu", thuTu);
    }

    fetch("http://localhost:8081/banner", {
        method: "POST",
        credentials: "include",
        body: formData
    })
        .then(async res => {
            if (!res.ok) {
                throw new Error(await res.text() || "Thêm banner thất bại");
            }

            return res.json();
        })
        .then(() => {
            alert("Thêm banner thành công");
            document.getElementById("bannerImage").value = "";
            document.getElementById("bannerThuTu").value = "";
            loadBanner();
        })
        .catch(err => {
            console.log(err);
            alert(err.message || "Thêm banner thất bại");
        });

}

function xoaBanner(id) {

    if (!confirm("Bạn có chắc muốn xóa banner này không?")) {
        return;
    }

    fetch(`http://localhost:8081/banner/${id}`, {
        method: "DELETE",
        credentials: "include"
    })
        .then(async res => {
            if (!res.ok) {
                throw new Error(await res.text() || "Xóa banner thất bại");
            }

            alert("Xóa banner thành công");
            loadBanner();
        })
        .catch(err => {
            console.log(err);
            alert(err.message || "Xóa banner thất bại");
        });

}

function hienThiPhim() {

    fetch("http://localhost:8081/phim")
        .then(res => res.json())
        .then(data => {

            danhSachPhim = data;
            renderPhim(danhSachPhim);
            renderPhimSelect(danhSachPhim);

        });

}

function renderPhimSelect(data) {

    let html = `
        <option value="">
            -- Chọn phim --
        </option>
    `;

    data.forEach(p => {

        html += `
            <option value="${p.maPhim}">
                ${p.tenPhim}
            </option>
        `;
    });

    document.getElementById("scMaPhim").innerHTML = html;

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

                ${renderTrailerButton(p)}

                <button
                    class="btn-sua"
                    onclick="suaPhimTuId(${p.maPhim})"
                >
                    Sửa
                </button>

                <button
                    class="btn-xoa"
                    onclick="xoaPhim(${p.maPhim})"
                >
                    Xóa
                </button>

            </div>
        `;
    });

    document.getElementById("dsPhim").innerHTML = html;

}

function loadDinhDang() {

    fetch("http://localhost:8081/dinhdang")
        .then(res => res.json())
        .then(data => {

            let html = `
                <option value="">
                    -- Chọn định dạng --
                </option>
            `;

            data.forEach(dd => {

                html += `
                    <option value="${dd.maDinhDang}">
                        ${dd.tenDinhDang}
                    </option>
                `;
            });

            document.getElementById("maDinhDang").innerHTML = html;

        });

}

function loadTheLoai() {

    fetch("http://localhost:8081/theloai")
        .then(res => res.json())
        .then(data => {

            let html = `
                <option value="">
                    -- Chọn thể loại --
                </option>
            `;

            data.forEach(tl => {

                html += `
                    <option value="${tl.maTheLoai}">
                        ${tl.tenTheLoai}
                    </option>
                `;
            });

            document.getElementById("maTheLoai").innerHTML = html;

        });

}

function loadPhong() {

    fetch("http://localhost:8081/phong")
        .then(res => res.json())
        .then(data => {

            danhSachPhong = data;

            let html = `
                <option value="">
                    -- Chọn phòng --
                </option>
            `;

            data.forEach(phong => {

                html += `
                    <option value="${phong.maPhong}">
                        ${phong.tenPhong || "Phòng"} (${phong.tongCho || 0} ghế)
                    </option>
                `;
            });

            document.getElementById("scMaPhong").innerHTML = html;

        });

}

function loadSuatChieu() {

    fetch("http://localhost:8081/admin/suatchieu")
        .then(res => res.json())
        .then(data => {

            danhSachSuatChieu = data;
            renderSuatChieu(danhSachSuatChieu);

        });

}

function loadThongTinChuyenKhoan() {

    fetch("http://localhost:8081/payment/bank-info", {
        credentials: "include"
    })
        .then(res => res.json())
        .then(data => {

            document.getElementById("maNganHang").value = data.bankCode || "";
            document.getElementById("tenTaiKhoan").value = data.accountName || "";
            document.getElementById("soTaiKhoan").value = data.accountNumber || "";

        })
        .catch(err => {

            console.log(err);

        });

}

function luuThongTinChuyenKhoan() {

    let nganHangSelect = document.getElementById("maNganHang");
    let nganHangOption = nganHangSelect.options[nganHangSelect.selectedIndex];

    let thongTin = {
        bankCode: nganHangSelect.value,
        bankName: nganHangOption?.dataset.name || nganHangOption?.text || "",
        accountName: document.getElementById("tenTaiKhoan").value,
        accountNumber: document.getElementById("soTaiKhoan").value
    };

    fetch("http://localhost:8081/payment/bank-info", {

        method: "PUT",
        credentials: "include",

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify(thongTin)

    })
        .then(async res => {

            if (!res.ok) {
                let message = await res.text();
                throw new Error(message);
            }

            return res.json();

        })
        .then(() => {

            alert("Lưu thông tin chuyển khoản thành công");

        })
        .catch(err => {

            console.log(err);
            alert(err.message || "Lưu thông tin chuyển khoản thất bại");

        });

}

function traCuuVe(maQr = null) {

    const input = document.getElementById("maQrVe");
    const value = (maQr ?? input.value).trim();

    if (!value) {
        hienThiKetQuaKiemVe({
            hopLe: false,
            message: "Vui lòng quét hoặc nhập mã QR"
        });
        return;
    }

    input.value = value;
    guiYeuCauKiemVe(
            "http://localhost:8081/admin/ve/kiem-tra",
            value,
            "Đang tra cứu vé...",
            false
    );

}

function xacNhanSuDungVe() {

    const value = document.getElementById("maQrVe").value.trim();

    if (!value) {
        hienThiKetQuaKiemVe({
            hopLe: false,
            message: "Vui lòng quét hoặc nhập mã QR trước khi xác nhận"
        });
        return;
    }

    if (!confirm("Xác nhận vé này đã được sử dụng?")) {
        return;
    }

    guiYeuCauKiemVe(
            "http://localhost:8081/admin/ve/xac-nhan",
            value,
            "Đang xác nhận sử dụng vé...",
            true
    );

}

function guiYeuCauKiemVe(url, maQr, loadingMessage, dungCameraSauKhiXong) {

    const result = document.getElementById("ketQuaKiemVe");
    result.className = "ticket-check-result muted";
    result.innerHTML = loadingMessage;

    fetch(url, {
        method: "POST",
        credentials: "include",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            maQr
        })
    })
        .then(async res => {
            const contentType = res.headers.get("content-type") || "";
            const data = contentType.includes("application/json")
                    ? await res.json()
                    : await res.text();

            if (!res.ok) {
                throw new Error(typeof data === "string" ? data : data.message);
            }

            return data;
        })
        .then(data => {
            hienThiKetQuaKiemVe(data);
            if (dungCameraSauKhiXong) {
                dungQuetQr();
            }
        })
        .catch(err => {
            console.log(err);
            hienThiKetQuaKiemVe({
                hopLe: false,
                message: err.message || "Vé không hợp lệ"
            });
        });

}

function hienThiKetQuaKiemVe(data) {

    const result = document.getElementById("ketQuaKiemVe");
    const hopLe = Boolean(data.hopLe);

    result.className = `ticket-check-result ${hopLe ? "valid" : "invalid"}`;
    result.innerHTML = `
        <h3>${hopLe ? "Vé hợp lệ" : "Vé không hợp lệ"}</h3>
        <p>${data.message || ""}</p>
        ${data.maVe ? `
            <p><b>Mã vé:</b> ${data.maVe}</p>
            <p><b>Phim:</b> ${data.tenPhim || ""}</p>
            <p><b>Phòng:</b> ${data.tenPhong || ""}</p>
            <p><b>Ghế:</b> ${data.soGhe || ""}</p>
            <p><b>Suất chiếu:</b> ${formatNgayGio(data.thoiGianBatDau)}</p>
            <p><b>Trạng thái:</b> ${data.trangThai || ""}</p>
        ` : ""}
    `;

}

async function batDauQuetQr() {

    const status = document.getElementById("qrScannerStatus");
    const video = document.getElementById("qrScannerVideo");

    if (!navigator.mediaDevices?.getUserMedia) {
        status.innerText = "Trình duyệt không hỗ trợ truy cập camera. Hãy dán nội dung mã QR vào ô nhập.";
        return;
    }

    try {
        qrDetector = "BarcodeDetector" in window
                ? new BarcodeDetector({ formats: ["qr_code"] })
                : null;

        if (!qrDetector && !window.jsQR) {
            status.innerText = "Không tải được bộ đọc QR. Hãy dán nội dung mã QR vào ô nhập.";
            return;
        }

        qrScannerStream = await navigator.mediaDevices.getUserMedia({
            video: {
                facingMode: "environment"
            }
        });

        video.srcObject = qrScannerStream;
        await video.play();

        status.innerText = "Đưa mã QR vào khung camera";
        quetQrTuCamera();
    } catch (err) {
        console.log(err);
        status.innerText = "Không bật được camera";
    }

}

function quetQrTuCamera() {

    const video = document.getElementById("qrScannerVideo");
    const status = document.getElementById("qrScannerStatus");

    if (!qrScannerStream) {
        return;
    }

    qrScannerTimer = window.setTimeout(async () => {
        try {
            const rawValue = qrDetector
                    ? await docQrBangBarcodeDetector(video)
                    : docQrBangCanvas(video);

            if (rawValue) {
                status.innerText = "Đã quét được mã QR";
                dungQuetQr();
                traCuuVe(rawValue);
                return;
            }
        } catch (err) {
            console.log(err);
        }

        quetQrTuCamera();
    }, 400);

}

async function docQrBangBarcodeDetector(video) {

    const codes = await qrDetector.detect(video);
    return codes.length > 0 ? codes[0].rawValue || "" : "";

}

function docQrBangCanvas(video) {

    if (!video.videoWidth || !video.videoHeight || !window.jsQR) {
        return "";
    }

    if (!qrCanvas) {
        qrCanvas = document.createElement("canvas");
        qrCanvasContext = qrCanvas.getContext("2d");
    }

    qrCanvas.width = video.videoWidth;
    qrCanvas.height = video.videoHeight;
    qrCanvasContext.drawImage(video, 0, 0, qrCanvas.width, qrCanvas.height);

    const imageData = qrCanvasContext.getImageData(0, 0, qrCanvas.width, qrCanvas.height);
    const code = window.jsQR(imageData.data, imageData.width, imageData.height);

    return code?.data || "";

}

function dungQuetQr() {

    if (qrScannerTimer) {
        window.clearTimeout(qrScannerTimer);
        qrScannerTimer = null;
    }

    if (qrScannerStream) {
        qrScannerStream.getTracks().forEach(track => track.stop());
        qrScannerStream = null;
    }

    const video = document.getElementById("qrScannerVideo");

    if (video) {
        video.srcObject = null;
    }

    const status = document.getElementById("qrScannerStatus");

    if (status) {
        status.innerText = "Camera đã tắt";
    }

}

function renderSuatChieu(data) {

    let html = "";

    data.forEach(sc => {

        html += `
            <div class="movie">

                <h3>${sc.tenPhim || "Phim"}</h3>

                <p>
                    <b>Phòng:</b>
                    ${sc.tenPhong || ""}
                </p>

                <p>
                    <b>Bắt đầu:</b>
                    ${formatNgayGio(sc.thoiGianBatDau)}
                </p>

                <p>
                    <b>Kết thúc:</b>
                    ${formatNgayGio(sc.thoiGianKetThuc)}
                </p>

                <button
                    class="btn-xoa"
                    onclick="xoaSuatChieu(${sc.maSuatChieu})"
                >
                    Xóa suất
                </button>

            </div>
        `;
    });

    document.getElementById("dsSuatChieu").innerHTML =
        html || `<p class="muted">Chưa có suất chiếu</p>`;

}

function themSuatChieu() {

    capNhatThoiGianKetThuc();

    let suatChieu = {
        maPhim: parseInt(document.getElementById("scMaPhim").value),
        maPhong: parseInt(document.getElementById("scMaPhong").value),
        thoiGianBatDau: document.getElementById("scBatDau").value,
        thoiGianKetThuc: document.getElementById("scKetThuc").value
    };

    if (!suatChieu.maPhim || !suatChieu.maPhong ||
        !suatChieu.thoiGianBatDau || !suatChieu.thoiGianKetThuc) {
        alert("Vui lòng nhập đủ thông tin suất chiếu");
        return;
    }

    fetch("http://localhost:8081/admin/suatchieu", {

        method: "POST",

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify(suatChieu)

    })
        .then(async res => {

            if (!res.ok) {
                let message = await res.text();
                throw new Error(message);
            }

            return res.json();

        })
        .then(() => {

            alert("Thêm suất chiếu thành công");
            resetSuatChieuForm();
            loadSuatChieu();

        })
        .catch(err => {

            console.log(err);
            alert(err.message || "Thêm suất chiếu thất bại");

        });

}

function capNhatThoiGianKetThuc() {

    let maPhim = parseInt(document.getElementById("scMaPhim").value);
    let batDau = document.getElementById("scBatDau").value;

    if (!maPhim || !batDau) {
        document.getElementById("scKetThuc").value = "";
        return;
    }

    let phim = danhSachPhim.find(p => p.maPhim === maPhim);

    if (!phim || !phim.thoiLuong) {
        document.getElementById("scKetThuc").value = "";
        return;
    }

    let thoiLuong = parseInt(phim.thoiLuong);

    if (Number.isNaN(thoiLuong)) {
        document.getElementById("scKetThuc").value = "";
        return;
    }

    let thoiGianKetThuc = new Date(batDau);
    thoiGianKetThuc.setMinutes(thoiGianKetThuc.getMinutes() + thoiLuong);

    document.getElementById("scKetThuc").value =
        formatDatetimeLocal(thoiGianKetThuc);

}

function xoaSuatChieu(id) {

    if (!confirm("Bạn có chắc muốn xóa suất chiếu này không?")) {
        return;
    }

    fetch(`http://localhost:8081/admin/suatchieu/${id}`, {
        method: "DELETE"
    })
        .then(() => {

            alert("Xóa suất chiếu thành công");
            loadSuatChieu();

        });

}

function themPhim() {

    let formData = layDuLieuFormData();

    fetch("http://localhost:8081/phim", {

        method: "POST",

        body: formData

    })
        .then(res => {

            if (!res.ok) {
                throw new Error("Lỗi thêm phim");
            }

            return res.json();

        })
        .then(() => {

            alert("Thêm phim thành công");
            hienThiPhim();
            resetForm();

        })
        .catch(err => {

            console.log(err);
            alert("Thêm phim thất bại");

        });

}

function suaPhimTuId(maPhim) {

    let phim = danhSachPhim.find(p => p.maPhim === maPhim);

    if (!phim) {
        alert("Không tìm thấy phim");
        return;
    }

    document.getElementById("maPhim").value = phim.maPhim;
    document.getElementById("tenPhim").value = phim.tenPhim || "";
    document.getElementById("thoiLuong").value = phim.thoiLuong || "";
    document.getElementById("moTa").value = phim.moTa || "";
    document.getElementById("doTuoi").value = phim.doTuoi || "";
    document.getElementById("trailer").value = phim.trailer || "";
    document.getElementById("maDinhDang").value = phim.maDinhDang?.maDinhDang || "";
    document.getElementById("maTheLoai").value = phim.maTheLoai?.maTheLoai || "";
    document.getElementById("poster").value = "";
    document.getElementById("posterHienTai").innerText =
        phim.anhPoster ? `Poster hiện tại: ${phim.anhPoster}` : "Chưa có poster";

    hienThiMucAdmin("movie", false);

    document.getElementById("formPhim").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });

    document.getElementById("tenPhim").focus();

}

function capNhatPhim() {

    let id = document.getElementById("maPhim").value;

    if (!id) {
        alert("Vui lòng bấm Sửa ở phim cần cập nhật trước");
        return;
    }

    let formData = layDuLieuFormData();

    fetch(`http://localhost:8081/phim/${id}`, {

        method: "PUT",

        body: formData

    })
        .then(res => {

            if (!res.ok) {
                throw new Error("Lỗi cập nhật");
            }

            return res.json();

        })
        .then(() => {

            alert("Cập nhật thành công");
            hienThiPhim();
            resetForm();

        })
        .catch(err => {

            console.log(err);
            alert("Cập nhật thất bại");

        });

}

function xoaPhim(id) {

    if (confirm("Bạn có chắc muốn xóa phim này không?")) {

        fetch(`http://localhost:8081/phim/${id}`, {

            method: "DELETE"

        })
            .then(() => {

                alert("Xóa thành công");
                hienThiPhim();

            });

    }

}

function layDuLieuFormData() {

    const formData = new FormData();
    const poster = document.getElementById("poster").files[0];

    formData.append("tenPhim", document.getElementById("tenPhim").value.trim());
    formData.append("thoiLuong", document.getElementById("thoiLuong").value.trim());
    formData.append("moTa", document.getElementById("moTa").value.trim());
    formData.append("doTuoi", document.getElementById("doTuoi").value.trim());
    formData.append("trailer", document.getElementById("trailer").value.trim());
    formData.append("maDinhDang", document.getElementById("maDinhDang").value);
    formData.append("maTheLoai", document.getElementById("maTheLoai").value);

    if (poster) {
        formData.append("poster", poster);
    }

    return formData;

}

function resetForm() {

    document.getElementById("maPhim").value = "";
    document.getElementById("tenPhim").value = "";
    document.getElementById("thoiLuong").value = "";
    document.getElementById("moTa").value = "";
    document.getElementById("doTuoi").value = "";
    document.getElementById("trailer").value = "";
    document.getElementById("poster").value = "";
    document.getElementById("posterHienTai").innerText = "";
    document.getElementById("maDinhDang").value = "";
    document.getElementById("maTheLoai").value = "";

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

function resetSuatChieuForm() {

    document.getElementById("scMaPhim").value = "";
    document.getElementById("scMaPhong").value = "";
    document.getElementById("scBatDau").value = "";
    document.getElementById("scKetThuc").value = "";

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

function formatDatetimeLocal(date) {

    let nam = date.getFullYear();
    let thang = String(date.getMonth() + 1).padStart(2, "0");
    let ngay = String(date.getDate()).padStart(2, "0");
    let gio = String(date.getHours()).padStart(2, "0");
    let phut = String(date.getMinutes()).padStart(2, "0");

    return `${nam}-${thang}-${ngay}T${gio}:${phut}`;

}

function dangXuat() {

    localStorage.removeItem("user");
    window.location.href = "login.html";

}
