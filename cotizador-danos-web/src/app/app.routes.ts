import { Routes } from '@angular/router';

export const routes: Routes = [
	{
		path: '',
		pathMatch: 'full',
		loadComponent: () =>
			import('./features/home/home.component').then(
				(m) => m.HomeComponent
			)
	},
	{
		path: 'cotizador',
		loadComponent: () =>
			import('./features/quote-creation/quote-creation.component').then(
				(m) => m.QuoteCreationComponent
			)
	},
	{
		path: 'quotes/:folio/general-info',
		loadComponent: () =>
			import('./features/quote-creation/quote-creation.component').then(
				(m) => m.QuoteCreationComponent
			)
	},
	{
		path: 'quotes/:folio/locations',
		loadComponent: () =>
			import('./features/quote-creation/quote-creation.component').then(
				(m) => m.QuoteCreationComponent
			)
	},
	{
		path: 'quotes/:folio/technical-info',
		loadComponent: () =>
			import('./features/quote-creation/quote-creation.component').then(
				(m) => m.QuoteCreationComponent
			)
	},
	{
		path: 'quotes/:folio/terms-and-conditions',
		loadComponent: () =>
			import('./features/quote-creation/quote-creation.component').then(
				(m) => m.QuoteCreationComponent
			)
	},
	{
		path: 'quotes/:folio/calculation-summary',
		loadComponent: () =>
			import('./features/quote-creation/quote-creation.component').then(
				(m) => m.QuoteCreationComponent
			)
	},
	{
		path: 'quotes/:folio/confirmation',
		loadComponent: () =>
			import('./features/quote-creation/quote-creation.component').then(
				(m) => m.QuoteCreationComponent
			)
	},
	{
		path: 'quote-creation',
		redirectTo: 'cotizador',
		pathMatch: 'full'
	},
	{
		path: '**',
		redirectTo: 'cotizador'
	}
];
