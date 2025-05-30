async function updateInfoDetails() {
	let fullname = document.getElementById("fullname").value.trim();
	let mobile = document.getElementById("mobile").value.trim();
	let address = document.getElementById("address").value.trim();
	let container = document.getElementById("message");
	if (!fullname || !mobile || !address) {
		container.innerText = "Nhập thiếu dữ liệu";
		container.style.color = "red";
		return;
	}
	if (mobile.length !== 10) {
		container.innerText = "Số điện thoại phải chính xác 10 số";
		container.style.color = "red";
		return;
	}
	try {
		container.innerText = "";
		const response = await fetch(`/api/updateUserInfor`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({
				fullname: fullname,
				mobile: mobile,
				address: address
			})
		});
		if (response.ok) {
			container.innerText = "Cập nhật thành công";
			container.style.color = "green";
		} else {
			const data = await response.json();
			console.log(data.message);
			container.innerText = data.message;
			container.style.color = "red";
		}
	} catch (error) {
		container.innerText = "Lỗi cập nhật";
	}
}
