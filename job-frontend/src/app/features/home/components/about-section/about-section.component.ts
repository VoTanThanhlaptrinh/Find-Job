import {
  Component,
  ElementRef,
  Inject,
  OnInit,
  PLATFORM_ID,
  AfterViewInit,
  OnDestroy,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

export interface BenefitItem {
  id: string;
  titleKey: string;
  descKey: string;
  iconType: 'ai' | 'verified' | 'consulting' | 'security';
}

@Component({
  selector: 'app-about-section',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './about-section.component.html',
  styleUrl: './about-section.component.css',
})
export class AboutSectionComponent implements OnInit, AfterViewInit, OnDestroy {
  isVisible = false;
  private observer?: IntersectionObserver;

  // Stat Count Up Display Values
  candidateCount = 0;
  satisfactionRate = 0;
  partnerCount = 0;

  benefits: BenefitItem[] = [
    {
      id: 'ai',
      titleKey: 'home.aboutSection.benefits.ai.title',
      descKey: 'home.aboutSection.benefits.ai.desc',
      iconType: 'ai',
    },
    {
      id: 'verified',
      titleKey: 'home.aboutSection.benefits.verified.title',
      descKey: 'home.aboutSection.benefits.verified.desc',
      iconType: 'verified',
    },
    {
      id: 'consulting',
      titleKey: 'home.aboutSection.benefits.consulting.title',
      descKey: 'home.aboutSection.benefits.consulting.desc',
      iconType: 'consulting',
    },
    {
      id: 'security',
      titleKey: 'home.aboutSection.benefits.security.title',
      descKey: 'home.aboutSection.benefits.security.desc',
      iconType: 'security',
    },
  ];

  avatars = [
    'assets/web_css/img/pages/t1.jpg',
    'assets/web_css/img/pages/t2.jpg',
    'assets/web_css/img/pages/t3.jpg',
    'assets/web_css/img/pages/t4.jpg',
    'assets/images/avatar.jpg',
  ];

  constructor(
    private el: ElementRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.isVisible = true;
      this.candidateCount = 100;
      this.satisfactionRate = 98;
      this.partnerCount = 5000;
    }
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      // Check prefers-reduced-motion
      const prefersReducedMotion = window.matchMedia(
        '(prefers-reduced-motion: reduce)'
      ).matches;

      if (prefersReducedMotion) {
        this.isVisible = true;
        this.candidateCount = 100;
        this.satisfactionRate = 98;
        this.partnerCount = 5000;
        return;
      }

      this.observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting && !this.isVisible) {
              this.isVisible = true;
              this.animateStats();
              this.observer?.disconnect();
            }
          });
        },
        { threshold: 0.15 }
      );

      this.observer.observe(this.el.nativeElement);
    }
  }

  private animateStats(): void {
    const duration = 1500;
    const start = performance.now();

    const frame = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      // Ease out cubic
      const easeOut = 1 - Math.pow(1 - progress, 3);

      this.candidateCount = Math.floor(easeOut * 100);
      this.satisfactionRate = Math.floor(easeOut * 98);
      this.partnerCount = Math.floor(easeOut * 5000);

      if (progress < 1) {
        requestAnimationFrame(frame);
      } else {
        this.candidateCount = 100;
        this.satisfactionRate = 98;
        this.partnerCount = 5000;
      }
    };

    requestAnimationFrame(frame);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
