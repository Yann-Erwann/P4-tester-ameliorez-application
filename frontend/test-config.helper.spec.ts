import { InjectionToken } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { configureTests } from './test-config.helper';

describe('configureTests', () => {
  it('configures and resolves a usable TestBed with the requested providers', async () => {
    const token = new InjectionToken<string>('configured value');
    const testBed = await configureTests(bed => {
      bed.configureTestingModule({ providers: [{ provide: token, useValue: 'ready' }] });
    });

    expect(testBed).toBe(TestBed);
    expect(testBed.inject(token)).toBe('ready');
  });
});
