import { Observable, of, throwError } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { CoreCatalogService } from '../../../core/services/core-catalog.service';
import { LocationDialogComponent, LocationDialogData } from './location-dialog.component';

describe('LocationDialogComponent', () => {
  let closedValue: unknown;
  let validateZipCodeMock: (_zipCode: string) => Observable<unknown>;

  const dialogRefStub = {
    close: (value?: unknown) => {
      closedValue = value;
    }
  };

  const coreCatalogServiceStub = {
    getBusinessLines: () => of([{ _id: '1', code: 'COM', name: 'Comercio', status: 'ACTIVE' }]),
    validateZipCode: (zipCode: string) => validateZipCodeMock(zipCode)
  };

  const createComponent = async (data: LocationDialogData) => {
    await TestBed.configureTestingModule({
      imports: [LocationDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRefStub },
        { provide: CoreCatalogService, useValue: coreCatalogServiceStub }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(LocationDialogComponent);
    fixture.detectChanges();

    return fixture.componentInstance;
  };

  beforeEach(() => {
    closedValue = undefined;
    validateZipCodeMock = () =>
      of({
        is_valid: true,
        zip_code: '11011',
        city: 'Bogota',
        state: 'Cundinamarca',
        risk_zone: 'A'
      });
  });

  it('should initialize form with provided location', async () => {
    const component = await createComponent({
      location: {
        locationName: 'Sucursal Norte',
        zipCode: '11011',
        address: 'Av. 1',
        giro: 'COM',
        fireKey: 'FK-10',
        buildingValue: 100,
        contentsValue: 20
      },
      businessLines: [{ _id: '1', code: 'COM', name: 'Comercio', status: 'ACTIVE' }]
    });

    expect(component.locationForm.getRawValue()).toEqual({
      locationName: 'Sucursal Norte',
      zipCode: '11011',
      address: 'Av. 1',
      giro: 'COM',
      fireKey: 'FK-10',
      buildingValue: 100,
      contentsValue: 20
    });

    expect(component.businessLineCatalog().length).toBe(1);
  });

  it('should load business lines from service when not provided in dialog data', async () => {
    const component = await createComponent({});

    expect(component.businessLineCatalog()).toEqual([
      { _id: '1', code: 'COM', name: 'Comercio', status: 'ACTIVE' }
    ]);
  });

  it('should skip zip validation call for invalid zip format', async () => {
    const component = await createComponent({});

    component.locationForm.patchValue({ zipCode: 'ABC' });
    component.validateZipCode();

    expect(component.zipValidationMessage()).toBe('');
  });

  it('should set message when zip code is not valid', async () => {
    validateZipCodeMock = () =>
      of({
        is_valid: false,
        zip_code: '11011',
        city: null,
        state: null,
        risk_zone: null
      });

    const component = await createComponent({});

    component.locationForm.patchValue({ zipCode: '11011' });
    component.validateZipCode();

    expect(component.zipValidationMessage()).toBe('Código postal no válido en catálogo.');
  });

  it('should set success zip validation message', async () => {
    validateZipCodeMock = () =>
      of({
        is_valid: true,
        zip_code: '11011',
        city: 'Bogota',
        state: 'Cundinamarca',
        risk_zone: 'B'
      });

    const component = await createComponent({});

    component.locationForm.patchValue({ zipCode: '11011' });
    component.validateZipCode();

    expect(component.zipValidationMessage()).toBe('CP válido: Bogota, Cundinamarca (Riesgo: B)');
  });

  it('should set error message when zip validation request fails', async () => {
    validateZipCodeMock = () => throwError(() => new Error('service down'));

    const component = await createComponent({});

    component.locationForm.patchValue({ zipCode: '11011' });
    component.validateZipCode();

    expect(component.zipValidationMessage()).toBe('No se pudo validar el código postal.');
  });

  it('should close dialog when close is called', async () => {
    const component = await createComponent({});

    component.close();

    expect(closedValue).toBeUndefined();
  });

  it('should not close dialog when submit is called with invalid form', async () => {
    const component = await createComponent({});

    component.locationForm.patchValue({
      locationName: '',
      zipCode: '11',
      giro: ''
    });

    component.submit();

    expect(closedValue).toBeUndefined();
    expect(component.locationForm.invalid).toBe(true);
  });

  it('should close dialog with location payload when submit is valid', async () => {
    const component = await createComponent({ location: { id: 'loc-1' } });

    component.locationForm.patchValue({
      locationName: 'Sede Sur',
      zipCode: '11011',
      address: 'Calle 123',
      giro: 'COM',
      fireKey: 'FK-2',
      buildingValue: 250,
      contentsValue: 75
    });

    component.submit();

    expect(closedValue).toEqual({
      id: 'loc-1',
      locationName: 'Sede Sur',
      zipCode: '11011',
      address: 'Calle 123',
      giro: 'COM',
      fireKey: 'FK-2',
      buildingValue: 250,
      contentsValue: 75
    });
  });
});
