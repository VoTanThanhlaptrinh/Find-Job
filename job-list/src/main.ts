import hljs from 'highlight.js';
import 'highlight.js/styles/atom-one-dark.css';

hljs.configure({ languages: ['javascript', 'typescript', 'css', 'html'] });
(window as any).hljs = hljs;

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));


