import { Provider } from '@angular/core';
import { TestBed } from '@angular/core/testing';

type CompilerOptions = Partial<{
  providers: Provider[];
  useJit: boolean;
}>;
export type ConfigureFn = (testBed: TestBed) => void;

export const configureTests = (configure: ConfigureFn, compilerOptions: CompilerOptions = {}): Promise<TestBed> => {
  TestBed.configureCompiler(compilerOptions);
  configure(TestBed);
  return TestBed.compileComponents().then(() => TestBed);
};
