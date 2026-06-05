import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// Initialize highlight.js only when needed
async function initializeHighlightJs() {
  try {
    const hljs = (await import('highlight.js')).default;
    hljs.configure({ languages: ['javascript', 'typescript', 'css', 'html'] });
    (window as any).hljs = hljs;
  } catch (e) {
    console.warn('Failed to load highlight.js');
  }
}

initializeHighlightJs();

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));


