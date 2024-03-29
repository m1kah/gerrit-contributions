#!/bin/sh

include_docs=-Dgerrit.include-documentation=1

while [ $# -gt 0 ]
do
	case "$1" in
	--no-documentation|--without-documentation)
		include_docs=
		shift
		;;
	*)
		echo >&2 "usage: $0 [--without-documentation]"
		exit 1
	esac
done

git update-index -q --refresh

if test -n "$(git diff-index --name-only HEAD --)" \
|| test -n "$(git ls-files --others --exclude-standard)"
then
	echo >&2 "error: working directory is dirty, refusing to build"
	exit 1
fi

./tools/version.sh --release &&
mvn clean install $include_docs -P all
rc=$?
./tools/version.sh --reset

if test 0 = $rc
then
	echo
	echo Built Gerrit Code Review `git describe`:
	ls gerrit-war/target/gerrit-*.war
	echo
fi
exit $rc
