import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { LayoutDashboard, Upload, BarChart3, Users, Shield, Moon, Sun, LogOut, UserCircle } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'

export default function Navbar() {
    const { user, logout, theme, toggleTheme } = useAuth()
    const navigate = useNavigate()

    const handleLogout = () => {
        logout()
        navigate('/login')
    }

    return (
        <nav className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-6 py-3 flex items-center justify-between">
            <Link to="/" className="font-bold text-lg text-brand-600 dark:text-brand-500">
                AI Code Review Assistant
            </Link>

            <div className="flex items-center gap-4">
                <button
                    onClick={toggleTheme}
                    className="flex items-center gap-1.5 text-sm px-3 py-1 rounded-md border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                >
                    {theme === 'light' ? <><Moon size={14} /> Dark</> : <><Sun size={14} /> Light</>}
                </button>

                {user ? (
                    <>
                        <Link to="/dashboard" className="flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-200 hover:text-brand-600 transition-colors">
                            <LayoutDashboard size={16} /> Dashboard
                        </Link>
                        <Link to="/upload" className="flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-200 hover:text-brand-600 transition-colors">
                            <Upload size={16} /> New Review
                        </Link>
                        <Link to="/analytics" className="flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-200 hover:text-brand-600 transition-colors">
                            <BarChart3 size={16} /> Analytics
                        </Link>
                        <Link to="/teams" className="flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-200 hover:text-brand-600 transition-colors">
                            <Users size={16} /> Teams
                        </Link>
                        {user.role === 'ROLE_ADMIN' && (
                            <Link to="/admin" className="flex items-center gap-1.5 text-sm text-purple-600 dark:text-purple-400 font-medium hover:underline">
                                <Shield size={16} /> Admin
                            </Link>
                        )}
                        <Link to="/profile" className="flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-200 hover:text-brand-600 transition-colors">
                            <UserCircle size={16} /> {user.name}
                        </Link>
                        <button onClick={handleLogout} className="flex items-center gap-1.5 text-sm bg-red-500 hover:bg-red-600 text-white px-3 py-1.5 rounded-md transition-colors">
                            <LogOut size={14} /> Logout
                        </button>
                    </>
                ) : (
                    <>
                        <Link to="/login" className="text-sm text-gray-700 dark:text-gray-200 hover:text-brand-600">Login</Link>
                        <Link to="/register" className="text-sm bg-brand-600 hover:bg-brand-700 text-white px-3 py-1.5 rounded-md">
                            Register
                        </Link>
                    </>
                )}
            </div>
        </nav>
    )
}