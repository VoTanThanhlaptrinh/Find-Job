# JobList

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 19.1.8.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

## Standard for Environment and Local Variables (Angular)

This project now uses a centralized API URL strategy so you do not need to edit service files when backend URLs change.

### 1. Environment files

- `src/environments/environment.ts`: base config entry point.
- `src/environments/environment.development.ts`: local/dev config.
- `src/environments/environment.production.ts`: production config.

Configured values:

- `apiBaseUrl`: the backend base URL used by frontend services.
- `allowLocalApiOverride`: allows overriding URL from browser local storage.

### 2. Build/serve behavior

- Development uses `environment.development.ts`.
- Production build uses `environment.production.ts`.

Angular file replacement is configured in `angular.json`, so no manual code edits are needed when switching environment.

### 3. Local override without code change

If you want to temporarily test another backend URL on your machine:

```js
localStorage.setItem('job-list.apiBaseUrl', 'https://your-backend-domain.com/api');
location.reload();
```

To remove override:

```js
localStorage.removeItem('job-list.apiBaseUrl');
location.reload();
```

### 4. Team convention

- Never hardcode API URL inside feature services.
- Always get API base URL through `UtilitiesService`.
- Keep `environment.production.ts` pointing to deployed backend URL.
- Use local storage override only for temporary local testing.
