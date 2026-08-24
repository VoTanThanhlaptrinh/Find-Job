import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Inject,
  OnDestroy,
  PLATFORM_ID,
  ViewChild,
} from '@angular/core';
import { RouterModule } from '@angular/router';
import Swiper from 'swiper';
import { Autoplay, Keyboard, Navigation, Pagination } from 'swiper/modules';

import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

export interface FeatureTab {
  id: number;
  titleKey: string;
  icon: string;
}

export interface FeatureSlide {
  id: number;
  titleKey: string;
  descKey: string;
  actionKey: string;
  actionRoute: string;
  type: 'search' | 'consulting' | 'apply' | 'track';
  bgGradient: string;
  borderColor: string;
  imageUrl: string;
  badgeKey: string;
  badgeIcon: string;
}

@Component({
  selector: 'app-features-section',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './features-section.component.html',
  styleUrl: './features-section.component.css',
})
export class FeaturesSectionComponent implements AfterViewInit, OnDestroy {
  @ViewChild('swiperRef') swiperRef!: ElementRef<HTMLElement>;
  swiperInstance: Swiper | null = null;
  activeTabIndex = 1; // Default to index 1

  tabs: FeatureTab[] = [
    { id: 0, titleKey: 'home.features.tabs.search', icon: 'search' },
    { id: 1, titleKey: 'home.features.tabs.consulting', icon: 'support_agent' },
    { id: 2, titleKey: 'home.features.tabs.apply', icon: 'send' },
    { id: 3, titleKey: 'home.features.tabs.track', icon: 'notifications' },
  ];

  slides: FeatureSlide[] = [
    {
      id: 0,
      titleKey: 'home.features.slides.search.title',
      descKey: 'home.features.slides.search.desc',
      actionKey: 'home.features.slides.search.action',
      actionRoute: '/jobs',
      type: 'search',
      bgGradient: 'from-blue-50/90 via-indigo-50/40 to-slate-50',
      borderColor: 'border-blue-100',
      imageUrl: 'assets/images/tim-kiem-thong-minh.png',
      badgeKey: 'home.features.slides.search.badge',
      badgeIcon: 'auto_awesome',
    },
    {
      id: 1,
      titleKey: 'home.features.slides.consulting.title',
      descKey: 'home.features.slides.consulting.desc',
      actionKey: 'home.features.slides.consulting.action',
      actionRoute: '/blogHome',
      type: 'consulting',
      bgGradient: 'from-indigo-50/90 via-blue-50/50 to-teal-50/40',
      borderColor: 'border-indigo-100',
      imageUrl: 'assets/images/tu-van-nghe-nghiep.png',
      badgeKey: 'home.features.slides.consulting.badge',
      badgeIcon: 'psychology',
    },
    {
      id: 2,
      titleKey: 'home.features.slides.apply.title',
      descKey: 'home.features.slides.apply.desc',
      actionKey: 'home.features.slides.apply.action',
      actionRoute: '/jobs',
      type: 'apply',
      bgGradient: 'from-emerald-50/90 via-teal-50/50 to-slate-50',
      borderColor: 'border-emerald-100',
      imageUrl: 'assets/images/ung-tuyen-nhanh.png',
      badgeKey: 'home.features.slides.apply.badge',
      badgeIcon: 'touch_app',
    },
    {
      id: 3,
      titleKey: 'home.features.slides.track.title',
      descKey: 'home.features.slides.track.desc',
      actionKey: 'home.features.slides.track.action',
      actionRoute: '/account/profile',
      type: 'track',
      bgGradient: 'from-amber-50/90 via-orange-50/40 to-slate-50',
      borderColor: 'border-amber-100',
      imageUrl: 'assets/images/theo-doi-ung-tuyen.png',
      badgeKey: 'home.features.slides.track.badge',
      badgeIcon: 'notifications_active',
    },
  ];

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private cdr: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId) && this.swiperRef) {
      this.initSwiper();
    }
  }

  initSwiper(): void {
    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    this.swiperInstance = new Swiper(this.swiperRef.nativeElement, {
      modules: [Navigation, Pagination, Autoplay, Keyboard],
      centeredSlides: true,
      loop: true,
      initialSlide: 1,
      speed: 500,
      grabCursor: true,
      keyboard: {
        enabled: true,
      },
      autoplay: prefersReducedMotion
        ? false
        : {
            delay: 5500,
            disableOnInteraction: false,
            pauseOnMouseEnter: true,
          },
      navigation: {
        prevEl: '.feature-swiper-prev',
        nextEl: '.feature-swiper-next',
      },
      pagination: {
        el: '.feature-swiper-pagination',
        clickable: true,
        bulletClass: 'feature-swiper-dot',
        bulletActiveClass: 'feature-swiper-dot-active',
      },
      slidesPerView: 1.15,
      spaceBetween: 16,
      breakpoints: {
        640: {
          slidesPerView: 1.25,
          spaceBetween: 20,
        },
        1024: {
          slidesPerView: 1.25,
          spaceBetween: 20,
        },
        1280: {
          slidesPerView: 1.35,
          spaceBetween: 24,
        },
      },
      on: {
        slideChange: (swiper) => {
          this.activeTabIndex = swiper.realIndex;
          this.cdr.detectChanges();
        },
      },
    });

    this.activeTabIndex = this.swiperInstance.realIndex ?? 1;
    this.cdr.detectChanges();
  }

  selectTab(index: number): void {
    this.activeTabIndex = index;
    if (this.swiperInstance) {
      this.swiperInstance.slideToLoop(index, 500);
    }
  }

  ngOnDestroy(): void {
    if (this.swiperInstance) {
      this.swiperInstance.destroy(true, true);
    }
  }
}
