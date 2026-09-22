import { Routes, Route, Navigate } from 'react-router';
import Layout from '@/components/Layout';
import Home from '@/pages/Home';
import Reading from '@/pages/Reading';
import SurahIndex from '@/pages/SurahIndex';
import JuzIndex from '@/pages/JuzIndex';
import Search from '@/pages/Search';
import Bookmarks from '@/pages/Bookmarks';
import SettingsPage from '@/pages/Settings';
import RecitationPage from '@/pages/Recitation';
import About from '@/pages/About';
import Help from '@/pages/Help';
import Khatm from '@/pages/Khatm';
import BookIndex from '@/pages/BookIndex';
import BookReader from '@/pages/BookReader';
import InstallPrompt from '@/components/InstallPrompt';

export default function App() {
  return (
    <Layout>
      <InstallPrompt />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/surah/:n" element={<Reading />} />
        <Route path="/surahs" element={<SurahIndex />} />
        <Route path="/juz" element={<JuzIndex />} />
        <Route path="/search" element={<Search />} />
        <Route path="/bookmarks" element={<Bookmarks />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/recitation" element={<RecitationPage />} />
        <Route path="/about" element={<About />} />
        <Route path="/help" element={<Help />} />
        <Route path="/khatm" element={<Khatm />} />
        <Route path="/book" element={<BookIndex />} />
        <Route path="/book/:lang/:n" element={<BookReader />} />
        {/* Catch-all: redirect unknown paths home. Essential for the Android
            WebView shell, whose entry URL is /assets/www/index.html — without
            this, no route matches and the app boots to a blank screen. */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
