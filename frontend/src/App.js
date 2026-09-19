import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import GlobalErrorBanner from './components/GlobalErrorBanner';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';

const Login = lazy(() => import('./pages/Login'));
const AlertQueue = lazy(() => import('./pages/AlertQueue'));
const AlertDetail = lazy(() => import('./pages/AlertDetail'));
const CaseList = lazy(() => import('./pages/CaseList'));
const CaseDetail = lazy(() => import('./pages/CaseDetail'));
const CustomerDetail = lazy(() => import('./pages/CustomerDetail'));
const AccountDetail = lazy(() => import('./pages/AccountDetail'));
const Rules = lazy(() => import('./pages/Rules'));
const Jobs = lazy(() => import('./pages/Jobs'));

function PageLoader() {
  return (
    <div className="min-h-screen bg-base flex items-center justify-center">
      <span className="text-text-muted text-sm">Loading…</span>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <GlobalErrorBanner />
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route element={<ProtectedRoute />}>
              <Route path="/alerts" element={<AlertQueue />} />
              <Route path="/alerts/:id" element={<AlertDetail />} />
              <Route path="/cases" element={<CaseList />} />
              <Route path="/cases/:id" element={<CaseDetail />} />
              <Route path="/customers/:id" element={<CustomerDetail />} />
              <Route path="/accounts/:id" element={<AccountDetail />} />
              <Route element={<AdminRoute />}>
                <Route path="/rules" element={<Rules />} />
                <Route path="/jobs" element={<Jobs />} />
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/alerts" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  );
}
