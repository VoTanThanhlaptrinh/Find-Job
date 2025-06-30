async function apply() {
	let fullname = document.getElementById("fullname").value.trim();
	let email = document.getElementById("email").value.trim();
	let tel = document.getElementById("tel").value.trim();
	let cv = document.getElementById("cv").value; // Giả sử đây là file upload, không cần trim
	let jobId = document.getElementById("job").value;
	let applyMessage = document.getElementById("apply-message");

	// Kiểm tra nếu thiếu thông tin (rỗng)
	if (!fullname || !email || !tel || !cv) {
		applyMessage.innerText = "Nhập thiếu thông tin";
		applyMessage.style.color = "red";
		return;
	}

	// Regex cho số điện thoại Việt Nam (10 chữ số, đầu số hợp lệ)
	const regex = /^(03[2-9]|05[6-9]|07[0-9]|08[1-9]|09[0-9])[0-9]{7}$/;
	if (!regex.test(tel)) {
		applyMessage.innerText = "Số điện thoại không hợp lệ";
		applyMessage.style.color = "red";
		return;
	}

	try {
		const response = await fetch('/api/apply/submit', {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ jobId: jobId, fullname: fullname, email: email, tel: tel })
		});
		const data = await response.json();
		

		toastr.options = {
			"closeButton": true,
			"progressBar": true,
			"positionClass": "toast-bottom-right",
			"timeOut": "2000" // 2 giây
		};
		if(response.ok){
			toastr.success(data.message);
		}else{
			
		}
	} catch (error) {
		toastr.error(error);
	}
}