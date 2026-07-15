import React, { createContext, useContext, useEffect, useState } from 'react'
import { loginUser, registerUser } from '../services/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = sessionStorage.getItem('user')
    return saved ? JSON.parse(saved) : null
  })
  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'light')

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    localStorage.setItem('theme', theme)
  }, [theme])

  const login = async (email, password) => {
    const { data } = await loginUser({ email, password })
    persistSession(data)
    return data
  }

  const register = async (name, email, password) => {
    const { data } = await registerUser({ name, email, password })
    persistSession(data)
    return data
  }

  const persistSession = (data) => {
    sessionStorage.setItem('token', data.token)
    const userData = { userId: data.userId, name: data.name, email: data.email, role: data.role }
    sessionStorage.setItem('user', JSON.stringify(userData))
    setUser(userData)
  }

  const logout = () => {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
    setUser(null)
  }

  const toggleTheme = () => setTheme((t) => (t === 'light' ? 'dark' : 'light'))

  return (
    <AuthContext.Provider value={{ user, login, register, logout, theme, toggleTheme }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
