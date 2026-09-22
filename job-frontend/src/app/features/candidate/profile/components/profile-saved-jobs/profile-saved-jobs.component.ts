import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of, take } from 'rxjs';
import { SavedJobsService } from '../../../../../core/services/saved-jobs.service';
import { UtilitiesService } from '../../../../../core/services/utilities.service';
import { JobCardComponent } from '../../../../../shared/components/job-card/job-card.component';
import { JobCardModel } from '../../../../../shared/models/jobs/job-card.model';

@Component({
  selector: 'app-profile-saved-jobs',
  standalone: true,
  imports: [CommonModule, RouterLink, JobCardComponent],
  templateUrl: './profile-saved-jobs.component.html',
  styleUrl: './profile-saved-jobs.component.css'
})
export class ProfileSavedJobsComponent implements OnInit {
  private readonly savedJobsService = inject(SavedJobsService);
  private readonly utilities = inject(UtilitiesService);
  private readonly http = inject(HttpClient);

  private readonly apiUrl = this.utilities.getURLDev();

  savedJobs: JobCardModel[] = [];
  isLoadingSavedJobs = false;

  get savedJobCount(): number {
    return this.savedJobsService.savedJobIds().size;
  }

  ngOnInit(): void {
    this.loadSavedJobs();
  }

  loadSavedJobs(): void {
    const ids = Array.from(this.savedJobsService.savedJobIds());
    if (ids.length === 0) {
      this.savedJobs = [];
      this.isLoadingSavedJobs = false;
      return;
    }

    this.isLoadingSavedJobs = true;
    const requests = ids.map(id =>
      this.http.get<any>(`${this.apiUrl}/jobs/${id}`).pipe(
        take(1),
        catchError(() => of(null))
      )
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        this.savedJobs = results
          .filter(res => res && res.data)
          .map(res => {
            const d = res.data;
            return {
              id: d.id,
              title: d.title,
              address: d.address,
              salary: d.salary,
              time: d.time,
              companyName: d.companyName || 'Doanh nghiệp tuyển dụng',
              logoUrl: d.companyLogo || 'assets/web_css/img/post.png',
              isSaved: true
            } as JobCardModel;
          });
        this.isLoadingSavedJobs = false;
      },
      error: () => {
        this.isLoadingSavedJobs = false;
      }
    });
  }
}
