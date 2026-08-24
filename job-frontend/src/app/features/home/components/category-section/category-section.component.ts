import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  Inject,
  OnDestroy,
  PLATFORM_ID,
  ViewChild,
} from '@angular/core';
import { RouterModule } from '@angular/router';

export interface CareerRole {
  id: string;
  title: string;
  icon: string;
  colorClass: string;
  shadowClass: string;
  floatClass: string;
  isFeatured?: boolean;
}

export interface FloatingCategoryBadge {
  id: number;
  slug: string;
  title: string;
  icon: string;
  positionClasses: string;
  badgeClasses: string;
  iconBgClasses: string;
  iconAnimationClass: string;
  chipFloatClass: string;
  enterDelayClass: string;
  link: string;
}

@Component({
  selector: 'app-category-section',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './category-section.component.html',
  styleUrl: './category-section.component.css',
})
export class CategorySectionComponent implements AfterViewInit, OnDestroy {
  @ViewChild('sectionRef') sectionRef!: ElementRef<HTMLElement>;

  isSectionVisible = false;
  activeRoleId = 'expert'; // Default active: 'Chuyên gia'
  private observer: IntersectionObserver | null = null;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  roles: CareerRole[] = [
    {
      id: 'developer',
      title: 'Developer',
      icon: 'terminal',
      colorClass: 'bg-indigo-600 text-white',
      shadowClass: 'shadow-indigo-600/30',
      floatClass: 'role-card-dev',
      isFeatured: false,
    },
    {
      id: 'expert',
      title: 'Chuyên gia',
      icon: 'psychology',
      colorClass: 'bg-[#1a56db] text-white',
      shadowClass: 'shadow-[#1a56db]/40',
      floatClass: 'role-card-expert',
      isFeatured: true,
    },
    {
      id: 'engineer',
      title: 'Kỹ sư',
      icon: 'engineering',
      colorClass: 'bg-emerald-600 text-white',
      shadowClass: 'shadow-emerald-600/30',
      floatClass: 'role-card-eng',
      isFeatured: false,
    },
    {
      id: 'manager',
      title: 'Quản lý',
      icon: 'badge',
      colorClass: 'bg-sky-500 text-white',
      shadowClass: 'shadow-sky-500/30',
      floatClass: 'role-card-mgr',
      isFeatured: false,
    },
  ];

  badges: FloatingCategoryBadge[] = [
    {
      id: 1,
      slug: 'code',
      title: 'Lập trình',
      icon: 'code',
      positionClasses: '-top-5 left-3 sm:-top-6 sm:left-6 md:left-8',
      badgeClasses: 'border-indigo-100 bg-white/95 text-indigo-900 hover:border-indigo-300 shadow-indigo-500/10',
      iconBgClasses: 'bg-indigo-50 text-indigo-600',
      iconAnimationClass: 'anim-icon-code',
      chipFloatClass: 'chip-float-1',
      enterDelayClass: 'enter-chip-1',
      link: '/jobs',
    },
    {
      id: 2,
      slug: 'eng',
      title: 'Kỹ thuật',
      icon: 'settings',
      positionClasses: '-top-5 right-3 sm:-top-6 sm:right-6 md:right-8',
      badgeClasses: 'border-emerald-100 bg-white/95 text-emerald-900 hover:border-emerald-300 shadow-emerald-500/10',
      iconBgClasses: 'bg-emerald-50 text-emerald-600',
      iconAnimationClass: 'anim-icon-gear',
      chipFloatClass: 'chip-float-2',
      enterDelayClass: 'enter-chip-2',
      link: '/jobs',
    },
    {
      id: 3,
      slug: 'media',
      title: 'Truyền thông',
      icon: 'campaign',
      positionClasses: 'top-[36%] -left-3 sm:-left-6 md:-left-8',
      badgeClasses: 'border-rose-100 bg-white/95 text-rose-900 hover:border-rose-300 shadow-rose-500/10',
      iconBgClasses: 'bg-rose-50 text-rose-600',
      iconAnimationClass: 'anim-icon-wave',
      chipFloatClass: 'chip-float-4',
      enterDelayClass: 'enter-chip-3',
      link: '/jobs',
    },
    {
      id: 4,
      slug: 'accounting',
      title: 'Kế toán',
      icon: 'calculate',
      positionClasses: 'top-[36%] -right-3 sm:-right-6 md:-right-8',
      badgeClasses: 'border-amber-100 bg-white/95 text-amber-900 hover:border-amber-300 shadow-amber-500/10',
      iconBgClasses: 'bg-amber-50 text-amber-600',
      iconAnimationClass: 'anim-icon-calc',
      chipFloatClass: 'chip-float-3',
      enterDelayClass: 'enter-chip-4',
      link: '/jobs',
    },
    {
      id: 5,
      slug: 'consulting',
      title: 'Tư vấn',
      icon: 'support_agent',
      positionClasses: '-bottom-5 left-3 sm:-bottom-6 sm:left-8 md:left-10',
      badgeClasses: 'border-purple-100 bg-white/95 text-purple-900 hover:border-purple-300 shadow-purple-500/10',
      iconBgClasses: 'bg-purple-50 text-purple-600',
      iconAnimationClass: 'anim-icon-headset',
      chipFloatClass: 'chip-float-5',
      enterDelayClass: 'enter-chip-5',
      link: '/jobs',
    },
    {
      id: 6,
      slug: 'mgmt',
      title: 'Quản lý',
      icon: 'trending_up',
      positionClasses: '-bottom-5 right-3 sm:-bottom-6 sm:right-8 md:right-10',
      badgeClasses: 'border-blue-100 bg-white/95 text-blue-900 hover:border-blue-300 shadow-blue-500/10',
      iconBgClasses: 'bg-blue-50 text-[#1a56db]',
      iconAnimationClass: 'anim-icon-chart',
      chipFloatClass: 'chip-float-6',
      enterDelayClass: 'enter-chip-6',
      link: '/jobs',
    },
  ];

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId) && this.sectionRef) {
      if ('IntersectionObserver' in window) {
        this.observer = new IntersectionObserver(
          (entries) => {
            const entry = entries[0];
            if (entry.isIntersecting) {
              this.isSectionVisible = true;
              this.observer?.disconnect();
            }
          },
          { threshold: 0.15 }
        );
        this.observer.observe(this.sectionRef.nativeElement);
      } else {
        this.isSectionVisible = true;
      }
    } else {
      this.isSectionVisible = true;
    }
  }

  setActiveRole(id: string): void {
    this.activeRoleId = id;
  }

  ngOnDestroy(): void {
    if (this.observer) {
      this.observer.disconnect();
    }
  }
}

