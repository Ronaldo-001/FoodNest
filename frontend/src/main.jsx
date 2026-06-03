import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'react-hot-toast'
import App from './App'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              background: 'hsl(220, 18%, 15%)',
              color: 'hsl(220, 15%, 96%)',
              border: '1px solid hsla(220, 15%, 96%, 0.14)',
              borderRadius: '0.625rem',
            },
            success: { iconTheme: { primary: 'hsl(158, 64%, 40%)', secondary: 'white' } },
            error:   { iconTheme: { primary: 'hsl(0, 72%, 51%)',   secondary: 'white' } },
          }}
        />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
)
