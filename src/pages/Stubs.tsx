import BackBar from '@/components/BackBar';

function Stub({ title }: { title: string }) {
  return (
    <div>
      <BackBar title={title} />
      <p className="px-4 py-10 text-center" style={{ color: 'var(--muted)', fontSize: 13 }}>
        Coming soon.
      </p>
    </div>
  );
}

export function Surahs() {
  return <Stub title="Surahs" />;
}
export function Juz() {
  return <Stub title="Juz" />;
}
export function SearchPage() {
  return <Stub title="Search" />;
}
export function BookmarksPage() {
  return <Stub title="Bookmarks" />;
}
export function SettingsPage() {
  return <Stub title="Settings" />;
}
export function Recitation() {
  return <Stub title="Recitation" />;
}
export function About() {
  return <Stub title="About & credits" />;
}
