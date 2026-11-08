import { AuthProvider } from './context/AuthProvider';
import { ToastProvider } from './context/ToastProvider';
import AppRouter from './routes/AppRouter';

function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <AppRouter />
      </AuthProvider>
    </ToastProvider>
  );
}

export default App;
