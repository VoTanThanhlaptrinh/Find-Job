import { Component, inject, PLATFORM_ID, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { I18nService } from '../../i18n/i18n.service';
import { AppLanguage } from '../../i18n/translations';
import { LegalModalComponent } from './legal-modal.component';

export interface FooterLinkItem {
  labelKey: string;
  type: 'route' | 'anchor' | 'legal';
  route?: string;
  queryParams?: Record<string, string>;
  fragment?: string;
  anchorId?: string;
  legalType?: 'privacy' | 'terms';
}

export interface FooterLinkGroup {
  id: string;
  titleKey: string;
  items: FooterLinkItem[];
}

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe, MatDialogModule],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css',
})
export class FooterComponent {
  readonly currentYear = new Date().getFullYear();
  private readonly router = inject(Router);
  private readonly i18nService = inject(I18nService);
  private readonly dialog = inject(MatDialog);
  private readonly platformId = inject(PLATFORM_ID);

  // Accordion state for mobile devices
  readonly openGroupIndex = signal<number | null>(null);

  // Structured Link Groups for clean mapping and easy maintenance
  readonly linkGroups: FooterLinkGroup[] = [
    {
      id: 'explore',
      titleKey: 'footer.groupExplore',
      items: [
        { labelKey: 'footer.jobSearch', type: 'route', route: '/category' },
        { labelKey: 'footer.internFresher', type: 'route', route: '/category', queryParams: { level: 'intern-fresher' } },
        { labelKey: 'footer.remoteHybrid', type: 'route', route: '/category', queryParams: { workType: 'remote-hybrid' } },
        { labelKey: 'footer.exploreCategories', type: 'route', route: '/category', fragment: 'categories' },
      ],
    },
    {
      id: 'about',
      titleKey: 'footer.groupAbout',
      items: [
        { labelKey: 'footer.aboutUs', type: 'anchor', anchorId: 'about' },
        { labelKey: 'footer.careerBlog', type: 'route', route: '/blogHome' },
        { labelKey: 'footer.contactUs', type: 'anchor', anchorId: 'contact' },
      ],
    },
    {
      id: 'support',
      titleKey: 'footer.groupSupport',
      items: [
        { labelKey: 'footer.helpCenter', type: 'anchor', anchorId: 'contact' },
        { labelKey: 'footer.myAccount', type: 'route', route: '/infor/profile' },
        { labelKey: 'footer.privacyPolicy', type: 'legal', legalType: 'privacy' },
        { labelKey: 'footer.termsOfService', type: 'legal', legalType: 'terms' },
      ],
    },
    {
      id: 'employers',
      titleKey: 'footer.groupEmployers',
      items: [
        { labelKey: 'footer.postJob', type: 'route', route: '/recruiter/jobs/post-job' },
        { labelKey: 'footer.findCandidates', type: 'route', route: '/recruiter/candidates' },
        { labelKey: 'footer.employerRegister', type: 'route', route: '/recruiter/register' },
        { labelKey: 'footer.employerPortal', type: 'route', route: '/recruiter/login' },
      ],
    },
  ];

  get currentLanguage(): AppLanguage {
    return this.i18nService.currentLanguage;
  }

  switchLanguage(lang: AppLanguage): void {
    this.i18nService.setLanguage(lang);
  }

  toggleGroup(index: number): void {
    this.openGroupIndex.update((current) => (current === index ? null : index));
  }

  isGroupOpen(index: number): boolean {
    return this.openGroupIndex() === index;
  }

  handleLinkClick(item: FooterLinkItem): void {
    if (item.type === 'anchor' && item.anchorId) {
      this.navigateToAnchor(item.anchorId);
    } else if (item.type === 'legal' && item.legalType) {
      this.openLegalModal(item.legalType);
    } else if (item.type === 'route' && item.route) {
      this.router.navigate([item.route], {
        queryParams: item.queryParams,
        fragment: item.fragment,
      });
    }
  }

  navigateToAnchor(targetId: string): void {
    const isHome =
      this.router.url === '/' ||
      this.router.url.startsWith('/#') ||
      this.router.url.startsWith('/?');

    if (isHome) {
      this.scrollToElement(targetId);
    } else {
      this.router.navigate(['/'], { fragment: targetId });
    }
  }

  private scrollToElement(id: string): void {
    if (isPlatformBrowser(this.platformId)) {
      const el = document.getElementById(id);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }
  }

  openLegalModal(type: 'privacy' | 'terms'): void {
    this.dialog.open(LegalModalComponent, {
      width: 'auto',
      maxWidth: '92vw',
      panelClass: 'legal-dialog-panel',
      data: { type },
    });
  }
}
