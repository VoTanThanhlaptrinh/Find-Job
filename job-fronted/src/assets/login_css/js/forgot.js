let email = document.getElementById("email");
let div = document.getElementById("message") || document.createElement("p");
let countId = document.getElementById("countId");
async function mailCode() {
	if (email.value === "") {
		/*div.style.color = "red";
		div.innerText = "";
		div.innerText = "Không được để Email trống";*/
		toastr.options = {
			"closeButton": true,
			"progressBar": true,
			"positionClass": "toast-top-right",
			"timeOut": "2000" // 2 giây
		};
		toastr.error("Bạn chưa nhập email");
	} else {
		try {
			if (localStorage.getItem("interval") !== null) {
				clearInterval(localStorage.getItem("interval"));
				countId.innerText = "";
			}
			const response = await fetch(`/api/code/${email.value}`);
			const data = await response.json();
			localStorage.setItem("email", email.value);
			countDown();
			div.innerText = "";
			div.innerText = data.mess;
			div.style.color = response.ok ? "green" : "red";
		} catch (error) {
			div.innerText = "";
			div.innerText = "Lỗi xác thực";
			div.style.color = "red";
			console.log(error)
		}

	}
}
async function verify() {
	if (email.value === "") {
		div.style.color = "red";
		div.innerText = "";
		div.innerText = "Không được để Email trống";
	} else {
		try {
			let code = document.getElementById("code").value;
			if (code === "") {
				div.style.color = "red";
				div.innerText = "";
				div.innerText = "Mã xác thực trống";
			} else {
				const req = { userMail: email.value, value: code }
				const response = await fetch(`/api/verify`, {
					method: "POST",
					headers: { "Content-Type": "application/json" },
					body: JSON.stringify(req)
				});

				const data = await response.json();
				div.innerText = "";
				div.innerText = data.mess;
				if (response.ok) {
					div.style.color = "green";
					window.location.href = data.link;
				} else {
					div.style.color = "red";
				}
			}
		} catch (error) {
			div.innerText = "";
			div.innerText = "Lỗi xác thực";
			div.style.color = "red";
		}
	}
}

function countDown() {
	let timeout = 60;
	console.log("run countdown");
	const intervalId = setInterval(() => {
		timeout -= 1;
		countId.innerText = "Mã sẽ hết hạn sau: " + timeout;
		if (timeout < 1) {
			localStorage.removeItem("email");
			stopCountDown(intervalId);
			div.innerText = "";
		}
	}, 1000);
	localStorage.setItem("interval", intervalId);
}
function stopCountDown(intervalId) {
	clearInterval(intervalId);
	countId.innerText = "Mã xác nhận đã hết hạn";
}
