import { Component, AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-loading',
  standalone: true,
  templateUrl: './loading.component.html',
  styleUrl: './loading.component.css'
})
export class LoadingComponent implements AfterViewInit {

  // Hook này tự động chạy sau khi giao diện HTML đã được tải xong
  ngAfterViewInit(): void {
    // Micro-interaction: Update the steps to simulate real-time progress
    setTimeout(() => {
      // Xử lý Step 2
      const step2 = document.getElementById("step-2");

      // Ép kiểu về HTMLElement để có thể sử dụng thuộc tính .style
      const step2Icon = step2?.querySelector<HTMLElement>(".material-symbols-outlined");
      if (step2Icon) {
        step2Icon.textContent = "check_circle";
        step2Icon.classList.remove("animate-pulse", "text-primary");
        step2Icon.classList.add("text-secondary");
        step2Icon.style.fontVariationSettings = "'FILL' 1";
      }

      // Ép kiểu về HTMLSpanElement cho thẻ span
      const step2Text = step2?.querySelector<HTMLSpanElement>("span:last-child");
      if (step2Text) {
        step2Text.classList.add("text-on-surface-variant");
        step2Text.classList.remove("text-on-surface");
      }

      // Xử lý Step 3
      const step3 = document.getElementById("step-3");
      if (step3) {
        step3.classList.remove("opacity-40");
      }

      const step3Icon = step3?.querySelector<HTMLElement>(".material-symbols-outlined");
      if (step3Icon) {
        step3Icon.textContent = "sync";
        step3Icon.classList.add("animate-pulse", "text-primary");
        step3Icon.classList.remove("text-slate-400");
      }

      const step3Text = step3?.querySelector<HTMLSpanElement>("span:last-child");
      if (step3Text) {
        step3Text.classList.add("text-on-surface");
        step3Text.classList.remove("text-on-surface-variant");
      }
    }, 3000);
  }
}