import { Routes, Route } from 'react-router';
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
      </Routes>
    </Layout>
  );
}
